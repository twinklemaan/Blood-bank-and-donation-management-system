package com.bloodbank.model;

import java.util.List;

/**
 * Concrete user subclass for hospital representatives.
 * Can raise and track requests and search blood availability.
 */
public class HospitalUser extends User {
    private final int hospitalId;
    private String hospitalName;

    public HospitalUser(int userId, String username, int hospitalId) {
        super(userId, username, "HOSPITAL");
        this.hospitalId = hospitalId;
    }

    public HospitalUser(int userId, String username, int hospitalId, String hospitalName) {
        super(userId, username, "HOSPITAL");
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    @Override
    public List<String> getTabs() {
        return List.of(
                "Raise Request",
                "My Requests",
                "Availability Search"
        );
    }

    @Override
    public String getRoleDisplayName() {
        return "Hospital (" + (hospitalName != null ? hospitalName : "Hosp #" + hospitalId) + ")";
    }
}
