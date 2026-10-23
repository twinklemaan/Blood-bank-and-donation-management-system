package com.bloodbank.model;

/**
 * Model representing a Hospital entity.
 */
public class Hospital {
    private final int hospitalId;
    private final String name;
    private final String regNo;
    private final String phone;
    private final String city;
    private final String state;
    private final String pincode;

    public Hospital(int hospitalId, String name, String regNo, String phone, String city, String state, String pincode) {
        this.hospitalId = hospitalId;
        this.name = name;
        this.regNo = regNo;
        this.phone = phone;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public String getName() {
        return name;
    }

    public String getRegNo() {
        return regNo;
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
        return name + " (" + city + ", ID: " + hospitalId + ")";
    }
}
