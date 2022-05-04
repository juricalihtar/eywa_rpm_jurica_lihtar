package hr.java.eywa.model;

import java.util.List;

public class Company {

    private Boolean isClient;
    private Boolean isSupplier;
    private String name;
    private String oib;
    private String eori;
    private String vat;
    private String iban;
    private Address address;
    private List<Person> contactPersons;


    public Company(Boolean isClient, Boolean isSupplier, String name, String oib, String eori, String vat, String iban, Address address, List<Person> contactPersons) {
        this.isClient = isClient;
        this.isSupplier = isSupplier;
        this.name = name;
        this.oib = oib;
        this.eori = eori;
        this.vat = vat;
        this.iban = iban;
        this.address = address;
        this.contactPersons = contactPersons;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOib() {
        return oib;
    }

    public void setOib(String oib) {
        this.oib = oib;
    }

    public List<Person> getContactPersons() {
        return contactPersons;
    }

    public void setContactPersons(List<Person> contactPersons) {
        this.contactPersons = contactPersons;
    }

    public Boolean getIsClient() {
        return isClient;
    }

    public void setIsClient(Boolean client) {
        isClient = client;
    }

    public Boolean getiSSupplier() {
        return isSupplier;
    }

    public void setIsSupplier(Boolean supplier) {
        isSupplier = supplier;
    }

    public String getEori() {
        return eori;
    }

    public void setEori(String eori) {
        this.eori = eori;
    }

    public String getVat() {
        return vat;
    }

    public void setVat(String vat) {
        this.vat = vat;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Company{" +
                "name='" + name + '\'' +
                ", oib='" + oib + '\'' +
                ", contactPersons=" + contactPersons +
                '}';
    }
}
