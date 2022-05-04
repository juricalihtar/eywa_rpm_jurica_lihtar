package hr.java.eywa.main;

import hr.java.eywa.model.Address;
import hr.java.eywa.model.Company;
import hr.java.eywa.model.Person;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {


        List<Company> companies = new ArrayList<>();

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        driver.manage().window().maximize();
        driver.get("https://www.eywaonline.com/development/");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(10000));

        //Locating elements for login
        driver.findElement(By.id("username"));
        WebElement username = driver.findElement(By.id("username"));

        driver.findElement(By.id("password"));
        WebElement password = driver.findElement(By.id("password"));

        driver.findElement(By.xpath("//button[text()='Sign In']"));
        WebElement loginButton = driver.findElement(By.xpath("//button[text()='Sign In']"));

        //Login process
        username.sendKeys("RRobotic");
        password.sendKeys("change-me1");
        loginButton.click();
        logger.info("Successfully logged in\n");

        Thread.sleep(2000);
        driver.get("https://www.eywaonline.com/development/modules/clients/clients.php");
        logger.info("Module 'Kontakti' successfully opened\n");

        try {
            jsonReaderSuppliers(companies, driver);
            logger.info("Started scenario execution. Input data successfully read");

        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Error during managing json file");
        }

        Thread.sleep(1000);

        for (Company company : companies){
            try {
                logger.info("Started " + company.getName() + " task");
                managingCompanies(driver, company, wait);
                logger.info(company.getName() + " task successfully finished\n");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.info("Search all inserted companies:");
        for (Company company : companies){
            try {
                String typeKeywordPlural = "";
                WebElement tabLink;

                if(company.getiSSupplier()){
                    typeKeywordPlural = "suppliers";
                    tabLink = driver.findElement(By.xpath("/html/body/div[4]/div/div[1]/ul/li[1]/a"));
                }
                else{
                    typeKeywordPlural = "clients";
                    tabLink = driver.findElement(By.xpath("/html/body/div[4]/div/div[1]/ul/li[2]/a"));
                }
                tabLink.click();
                typeCompanyNamesInSearchbar(driver, company);

                Thread.sleep(2000);
                WebElement companyTableNameColumn = driver.findElement(By.xpath("//*[@id=\"" + typeKeywordPlural + "_table\"]/tbody/tr/td[1]/div"));
                String companyNameTable = companyTableNameColumn.getAttribute("data-title");
                if(companyNameTable.equals(company.getName())){
                    logger.info(company.getName() + " found in list");
                }
                else{
                    logger.info(company.getName() + "not found in list");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("All data from input was processed");

        driver.quit();
    }

    private static void managingCompanies(WebDriver driver, Company company, WebDriverWait wait) throws InterruptedException {

        String typeKeywordSingular = "";
        String typeKeywordPlural = "";
        WebElement tabLink;

        if(company.getiSSupplier()){
            typeKeywordSingular = "supplier";
            typeKeywordPlural = "suppliers";
            tabLink = driver.findElement(By.xpath("/html/body/div[4]/div/div[1]/ul/li[1]/a"));
        }
        else{
            typeKeywordSingular = "client";
            typeKeywordPlural = "clients";
            tabLink = driver.findElement(By.xpath("/html/body/div[4]/div/div[1]/ul/li[2]/a"));
        }
        tabLink.click();

        Thread.sleep(2000);

        //Selecting table length to "All"
        Select selectTableLength = new Select(driver.findElement(By.name(typeKeywordPlural + "_table_length")));
        selectTableLength.selectByValue("-1");

        Thread.sleep(1000);

        typeCompanyNamesInSearchbar(driver, company);

        Thread.sleep(2000);

        WebElement companiesTable = driver.findElement(By.id(typeKeywordPlural + "_table"));

        JavascriptExecutor jse = (JavascriptExecutor) driver;

        if(driver.findElement(By.xpath("//*[@id=\"" + typeKeywordPlural + "_table\"]/tbody/tr/td")).getText().equals("No matching records found")
                && !company.getName().isBlank() && !company.getAddress().getStreetName().isBlank() && (company.getIsClient() || company.getiSSupplier())){
            logger.info("Company " + company.getName() + " was not found in list. Creating new.");
            addNewCompanyFromFields(driver, company, wait, typeKeywordSingular, jse);
            saveAndGoBackToCompaniesTable(driver, wait, jse);

        }
        Thread.sleep(2000);
        WebElement companyTableNameColumn = driver.findElement(By.xpath("//*[@id=\"" + typeKeywordPlural + "_table\"]/tbody/tr/td[1]/div"));
        String companyNameTable = companyTableNameColumn.getAttribute("data-title");
        if(companyNameTable.equals(company.getName())){

            Thread.sleep(2000);
            WebElement openFoundCompany = driver.findElement(By.xpath("//*[@id=\""+ typeKeywordPlural + "_table\"]/tbody/tr"));
            openFoundCompany.click();

            for(int i = 0; i < company.getContactPersons().size(); i++) {

                WebElement companyContactCount = driver.findElement(By.xpath("//*[@id=\"client_contact_count\"]"));
                String numberOfContactsString = companyContactCount.getAttribute("value");
                Integer numberOfContacts = Integer.parseInt(numberOfContactsString);

                List<WebElement> contactFields;

                //In case when contact in input.json is blank
                if(company.getContactPersons().get(i).getFirstName().isBlank()
                        && company.getContactPersons().get(i).getLastName().isBlank()){
                    logger.info("Compare contacts for " + company.getName());
                    goBackToCompanies(driver);
                    logger.info(company.getName() + "has no contacts");
                    break;

                }
                //In case there are no contacts in the application and in input.json file contacts aren't blank
                if(numberOfContacts == 1) {
                    logger.info("Compare contacts for " + company.getName());
                    int contactsCounter = 0;
                    for(int j = 0; j < company.getContactPersons().size(); j++){
                        if(!company.getContactPersons().get(j).getFirstName().isBlank()
                                && !company.getContactPersons().get(j).getLastName().isBlank()){
                            addCompanyContacts(driver, company, jse, j);
                            contactsCounter++;
                            i++;

                        }
                        else{
                            goBackToCompanies(driver);
                            logger.info(company.getName() + " contacts are blank");
                            break;
                        }
                    }
                    if(contactsCounter > 0){
                        saveAndGoBackToCompaniesTable(driver, wait, jse);
                        logger.info("Added " + contactsCounter + " contacts to " + company.getName());
                    }


                }
                //In case when number of contacts in the application is the same size as contacts array in input.json file and contacts arent blank
                if(company.getContactPersons().size() == (numberOfContacts - 1) && !company.getContactPersons().get(i).getFirstName().isBlank()
                        && !company.getContactPersons().get(i).getLastName().isBlank() ){
                    logger.info("Compare contacts for " + company.getName());
                    //When there are more than one contact
                    if(company.getContactPersons().size() > 1){
                        for(int j = 0; j < (numberOfContacts - 1); j++){
                            contactFields = getContactFields(driver, j);
                            compareContactsSameSize(company, contactFields, j);
                             i++;
                        }
                        logger.info("Updated " + (numberOfContacts - 1) + " for " + company.getName());
                    }
                    //When there is only one contact
                    else{
                        contactFields = getContactFields(driver, i);
                        compareContactsSameSize(company, contactFields, i);
                        logger.info("Updated 1 contact for " + company.getName());
                    }
                    Thread.sleep(2000);
                    saveAndGoBackToCompaniesTable(driver, wait, jse);
                }

                if(company.getContactPersons().size() > (numberOfContacts - 1) && (numberOfContacts - 1) > 1){
                    int counter = 0;
                    logger.info("Compare contacts for " + company.getName());
                    for(int j = 0; j < (numberOfContacts - 1); j++) {
                        if(!company.getContactPersons().get(j).getFirstName().isBlank()
                                && !company.getContactPersons().get(j).getLastName().isBlank()){

                            contactFields = getContactFields(driver, j);
                            compareContactsSameSize(company, contactFields, j);
                            counter = 1;
                        }
                        i++;
                        
                    }
                    int contactsCounter = 0;
                    for(int j = i; j < company.getContactPersons().size(); j++) {
                        if(!company.getContactPersons().get(j).getFirstName().isBlank()
                                && !company.getContactPersons().get(j).getLastName().isBlank()){

                            addCompanyContacts(driver, company, jse, j);
                            contactsCounter++;
                            i++;
                        }
                    }
                    
                    saveAndGoBackToCompaniesTable(driver, wait, jse);
                    logger.info("Updated " + (numberOfContacts - 1) + " for " + company.getName());
                    logger.info("Added " + contactsCounter + " for " + company.getName());
                }
            }
        }
    }

    private static void typeCompanyNamesInSearchbar(WebDriver driver, Company company) throws InterruptedException {
        WebElement searchBar = driver.findElement(By.xpath("//*[@id=\"overall_search_input\"]"));
        searchBar.clear();
        Thread.sleep(1000);
        searchBar.sendKeys(company.getName());
        searchBar.sendKeys(Keys.ENTER);
    }

    private static void compareContactsSameSize(Company company, List<WebElement> contactFields, int j) {
        if(!contactFields.get(0).getText().equals(company.getContactPersons().get(j).getFirstName())){
            contactFields.get(0).clear();
            contactFields.get(0).sendKeys(company.getContactPersons().get(j).getFirstName());
        }
        if(!contactFields.get(1).getText().equals(company.getContactPersons().get(j).getLastName())){
            contactFields.get(1).clear();
            contactFields.get(1).sendKeys(company.getContactPersons().get(j).getLastName());
        }
        if(!contactFields.get(2).getText().equals(company.getContactPersons().get(j).getEmail())){
            contactFields.get(2).clear();
            contactFields.get(2).sendKeys(company.getContactPersons().get(j).getEmail());
        }
        if(!contactFields.get(3).getText().equals(company.getContactPersons().get(j).getPhoneNumber())){
            contactFields.get(3).clear();
            contactFields.get(3).sendKeys(company.getContactPersons().get(j).getPhoneNumber());
        }
        if(!contactFields.get(4).getText().equals(company.getContactPersons().get(j).getMobilePhoneNumber())){
            contactFields.get(4).clear();
            contactFields.get(4).sendKeys(company.getContactPersons().get(j).getMobilePhoneNumber());
        }
    }

    private static void saveAndGoBackToCompaniesTable(WebDriver driver, WebDriverWait wait, JavascriptExecutor jse) {

        WebElement saveCompanyButton = driver.findElement(By.xpath("//*[@id=\"save_button_ui\"]"));
        wait.until(ExpectedConditions.elementToBeClickable(saveCompanyButton));

        jse.executeScript("arguments[0].click()", saveCompanyButton);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("alert_box")));

        WebElement okButton = driver.findElement(By.xpath("//*[@id=\"ok_confirm\"]"));
        okButton.click();

        goBackToCompanies(driver);
    }

    private static void goBackToCompanies(WebDriver driver) {
        WebElement backToCompaniesTable = driver.findElement(By.xpath("/html/body/div[4]/div[1]/ul/li[1]/a"));
        backToCompaniesTable.click();
    }

    private static List<WebElement> getContactFields(WebDriver driver, int i) {

        List<WebElement> contactFields = new ArrayList<>();

        WebElement contactFName = driver.findElement(By.xpath("//*[@id=\"contact_fname_" + (i + 1) + "\"]"));
        WebElement contactLName = driver.findElement(By.xpath("//*[@id=\"contact_lname_" + (i + 1) + "\"]"));
        WebElement contactEmail = driver.findElement(By.xpath("//*[@id=\"contact_email_" + (i + 1) + "\"]"));
        WebElement contactPhoneNumber = driver.findElement(By.xpath("//*[@id=\"contact_landline_" + (i + 1) + "\"]"));
        WebElement contactMobilePhoneNumber = driver.findElement(By.xpath("//*[@id=\"contact_mobile_" + (i + 1) + "\"]"));

        contactFields.add(contactFName);
        contactFields.add(contactLName);
        contactFields.add(contactEmail);
        contactFields.add(contactPhoneNumber);
        contactFields.add(contactMobilePhoneNumber);

        return contactFields;
    }

    private static void addNewCompanyFromFields(WebDriver driver, Company company, WebDriverWait wait, String typeKeywordSingular, JavascriptExecutor jse) throws InterruptedException {

        WebElement addCompanyButton = driver.findElement(By.id("add_supplier_btn"));
        jse = (JavascriptExecutor) driver;
        Thread.sleep(1000);
        jse.executeScript("arguments[0].click()", addCompanyButton);

        WebElement checkBoxClientType = driver.findElement(By.xpath("//*[@id=\"is_" + typeKeywordSingular + "_data\"]/span"));
        checkBoxClientType.click();

        WebElement companyNameInput = driver.findElement(By.xpath("//*[@id=\"new_client_name\"]"));
        companyNameInput.sendKeys(company.getName());

        WebElement companyOibInput = driver.findElement(By.xpath("//*[@id=\"new_client_oib\"]"));
        companyOibInput.sendKeys(company.getOib());

        WebElement companyEoriInput = driver.findElement(By.xpath("//*[@id=\"new_client_eori_number\"]"));
        companyEoriInput.sendKeys(company.getEori());

        WebElement companyVatInput = driver.findElement(By.xpath("//*[@id=\"new_client_vat_number\"]"));
        companyVatInput.sendKeys(company.getVat());

        WebElement companyIbanInput = driver.findElement(By.xpath("//*[@id=\"new_client_iban_number\"]"));
        companyIbanInput.sendKeys(company.getIban());

        WebElement companyAddressInput = driver.findElement(By.xpath("//*[@id=\"new_client_address\"]"));
        companyAddressInput.sendKeys(company.getAddress().getStreetName());

        WebElement companyCityInput = driver.findElement(By.xpath("//*[@id=\"new_client_city\"]"));
        companyCityInput.sendKeys(company.getAddress().getCity());

        driver.findElement(By.xpath("//*[@id=\"country_dropdown_selected_chosen\"]/a/span")).click();
        WebElement companyCountryInput = driver.findElement(By.xpath("//*[@id=\"country_dropdown_selected_chosen\"]/div/div/input"));
        companyCountryInput.sendKeys(company.getAddress().getCountry());
        companyCountryInput.sendKeys(Keys.ENTER);

        int contactsCounter = 0;
        for(int i = 0; i < company.getContactPersons().size(); i++){

            if(!company.getContactPersons().get(i).getFirstName().isBlank() && !company.getContactPersons().get(i).getLastName().isBlank()){

                addCompanyContacts(driver, company, jse, i);
                contactsCounter++;
            }
        }
        logger.info("SUCCESS: " + company.getName() + " added to list. Added " + contactsCounter + " contacts to " + company.getName());
    }

    private static void addCompanyContacts(WebDriver driver, Company company, JavascriptExecutor jse, int i) throws InterruptedException {

        WebElement addCompanyContactFieldsButton = driver.findElement(By.xpath("//*[@id=\"client_form_wrapper\"]/div/div[2]/div/div[1]/div/button"));
        Thread.sleep(1000);
        jse.executeScript("arguments[0].click()", addCompanyContactFieldsButton);

        WebElement contactFName = driver.findElement(By.xpath("//*[@id=\"contact_fname_" + (i + 1) +"\"]"));
        contactFName.sendKeys(company.getContactPersons().get(i).getFirstName());

        WebElement contactLName = driver.findElement(By.xpath("//*[@id=\"contact_lname_" + (i + 1) +"\"]"));
        contactLName.sendKeys(company.getContactPersons().get(i).getLastName());

        WebElement contactEmail = driver.findElement(By.xpath("//*[@id=\"contact_email_" + (i + 1) +"\"]"));
        contactEmail.sendKeys(company.getContactPersons().get(i).getEmail());

        WebElement contactPhoneNumber = driver.findElement(By.xpath("//*[@id=\"contact_landline_" + (i + 1) +"\"]"));
        contactPhoneNumber.sendKeys(company.getContactPersons().get(i).getPhoneNumber());

        WebElement contactMobilePhoneNumber = driver.findElement(By.xpath("//*[@id=\"contact_mobile_" + (i + 1) +"\"]"));
        contactMobilePhoneNumber.sendKeys(company.getContactPersons().get(i).getMobilePhoneNumber());
    }

    private static void jsonReaderSuppliers(List<Company> companiesList, WebDriver driver) throws IOException {

        File inputFile = new File("dat/input.json");

        JSONParser jsonParser = new JSONParser();

        if(inputFile.exists()){
            logger.info("File inputFile exists");
            try(FileReader fileReader = new FileReader(inputFile)){

                Object obj = jsonParser.parse(fileReader);
                JSONArray companiesArray = (JSONArray) obj;
                for(Object company : companiesArray){
                    Company newCompany = parseCompanyObject((JSONObject) company);
                    companiesList.add(newCompany);
                }
                companiesArray.forEach( company -> parseCompanyObject( (JSONObject) company ) );

            } catch (ParseException parseException) {
                parseException.printStackTrace();
            }
        }
        else{
            logger.error("File inputFile doesn't exists");
            driver.quit();
        }


    }
    private static Company parseCompanyObject(JSONObject company) {

        JSONObject companyObject = (JSONObject) company.get("company");

        //Company
        Boolean isClient = (Boolean) companyObject.get("isClient");
        Boolean isSupplier = (Boolean) companyObject.get("isSupplier");
        String name = (String) companyObject.get("name");
        String oib = (String) companyObject.get("oib");
        String eori = (String) companyObject.get("eori");
        String vat = (String) companyObject.get("vat");
        String iban = (String) companyObject.get("iban");

        //Company address
        JSONObject companyAddress = (JSONObject) companyObject.get("address");
        String street = (String) companyAddress.get("street");
        String city = (String) companyAddress.get("city");
        String country = (String) companyAddress.get("country");

        Address newCompanyAddress = new Address(street, city, country);

        //Contact persons
        List<Person> contactPersons = new ArrayList<>();
        JSONArray contactPersonsArray = (JSONArray) companyObject.get("contactPersons");
        for (int i = 0; i < contactPersonsArray.size(); i++) {
            JSONObject contactPerson = (JSONObject) contactPersonsArray.get(i);

            String firstName = (String) contactPerson.get("firstName");
            String lastName = (String) contactPerson.get("lastName");
            String email = (String) contactPerson.get("email");
            String phoneNumber = (String) contactPerson.get("phoneNumber");
            String mobilePhoneNumber = (String) contactPerson.get("mobilePhoneNumber");

            Person newPerson = new Person(firstName, lastName, email, phoneNumber, mobilePhoneNumber);
            contactPersons.add(newPerson);
        }
        Company newCompany = new Company(isClient, isSupplier, name, oib, eori, vat, iban, newCompanyAddress, contactPersons);

        return newCompany;
    }


}
