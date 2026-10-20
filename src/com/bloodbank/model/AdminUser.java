package com.bloodbank.model;

import java.util.List;

/**
 * Concrete user subclass for system administrators.
 * Has full management capabilities across all banks, hospitals, and staff.
 */
public class AdminUser extends User {

    public AdminUser(int userId, String username) {
        super(userId, username, "ADMIN");
    }

    @Override
    public List<String> getTabs() {
        return List.of(
                "Banks",
                "Hospitals",
                "Staff",
                "Stock",
                "All Donations",
                "All Requests"
        );
    }

    @Override
    public String getRoleDisplayName() {
        return "Administrator";
    }
}
