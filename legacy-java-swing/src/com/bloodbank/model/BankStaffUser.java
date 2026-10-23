package com.bloodbank.model;

import java.util.List;

/**
 * Concrete user subclass for blood bank staff members.
 * Scoped to their specific blood bank.
 */
public class BankStaffUser extends User {
    private final int staffId;
    private final int bankId;
    private String bankName;
    private String staffName;

    public BankStaffUser(int userId, String username, int staffId, int bankId) {
        super(userId, username, "STAFF");
        this.staffId = staffId;
        this.bankId = bankId;
    }

    public BankStaffUser(int userId, String username, int staffId, int bankId, String bankName, String staffName) {
        super(userId, username, "STAFF");
        this.staffId = staffId;
        this.bankId = bankId;
        this.bankName = bankName;
        this.staffName = staffName;
    }

    public int getStaffId() {
        return staffId;
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

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    @Override
    public List<String> getTabs() {
        return List.of(
                "Donors",
                "Donations",
                "Screening",
                "Requests to Fulfil",
                "Stock"
        );
    }

    @Override
    public String getRoleDisplayName() {
        return "Bank Staff (" + (bankName != null ? bankName : "Bank #" + bankId) + ")";
    }
}
