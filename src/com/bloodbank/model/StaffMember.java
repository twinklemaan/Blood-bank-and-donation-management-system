package com.bloodbank.model;

import java.util.List;

/**
 * Concrete class representing a blood bank staff member.
 * Extends Person (Rubric: Inheritance).
 */
public class StaffMember extends Person {
    private final int bankId;
    private String bankName;
    private final String role;

    public StaffMember(int id, int bankId, String name, String role, String phone) {
        super(id, name, "", "", "", phone != null ? List.of(phone) : List.of());
        this.bankId = bankId;
        this.role = role;
    }

    public StaffMember(int id, int bankId, String bankName, String name, String role, String phone) {
        super(id, name, "", "", "", phone != null ? List.of(phone) : List.of());
        this.bankId = bankId;
        this.bankName = bankName;
        this.role = role;
    }

    public int getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return getPrimaryPhone();
    }

    @Override
    public String toString() {
        return getName() + " (" + role + ", ID: " + getId() + ")";
    }
}
