package com.bloodbank.model;

import java.time.LocalDateTime;

/**
 * Model representing a blood stock record for a bank and blood group.
 */
public class StockEntry {
    private final int bankId;
    private final String bankName;
    private final String city;
    private final String bloodGroup;
    private final int unitsAvailable;
    private final LocalDateTime lastUpdated;

    public StockEntry(int bankId, String bankName, String city, String bloodGroup, int unitsAvailable, LocalDateTime lastUpdated) {
        this.bankId = bankId;
        this.bankName = bankName;
        this.city = city;
        this.bloodGroup = bloodGroup;
        this.unitsAvailable = unitsAvailable;
        this.lastUpdated = lastUpdated;
    }

    public int getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getCity() {
        return city;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public int getUnitsAvailable() {
        return unitsAvailable;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return bankName + " - " + bloodGroup + ": " + unitsAvailable + " units";
    }
}
