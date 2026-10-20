package com.bloodbank.model;

/**
 * Model representing a Blood Bank organisation entity.
 */
public class Bank {
    private final int bankId;
    private final String name;
    private final String licenceNo;
    private final String phone;
    private final String city;
    private final String state;
    private final String pincode;

    public Bank(int bankId, String name, String licenceNo, String phone, String city, String state, String pincode) {
        this.bankId = bankId;
        this.name = name;
        this.licenceNo = licenceNo;
        this.phone = phone;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public int getBankId() {
        return bankId;
    }

    public String getName() {
        return name;
    }

    public String getLicenceNo() {
        return licenceNo;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    @Override
    public String toString() {
        return name + " (" + city + ", ID: " + bankId + ")";
    }
}
