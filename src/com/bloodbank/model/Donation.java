package com.bloodbank.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model representing a blood donation record.
 */
public class Donation {
    private final int donationId;
    private final int donorId;
    private String donorName;
    private String bloodGroup;
    private final int bankId;
    private String bankName;
    private final LocalDate donationDate;
    private final LocalDate expiryDate;
    private final int units;
    private final int unitsRemaining;
    private final BigDecimal hemoglobinLevel;
    private final String screeningResult;

    public Donation(int donationId, int donorId, String donorName, String bloodGroup,
                    int bankId, String bankName, LocalDate donationDate, LocalDate expiryDate,
                    int units, int unitsRemaining, BigDecimal hemoglobinLevel, String screeningResult) {
        this.donationId = donationId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.bloodGroup = bloodGroup;
        this.bankId = bankId;
        this.bankName = bankName;
        this.donationDate = donationDate;
        this.expiryDate = expiryDate;
        this.units = units;
        this.unitsRemaining = unitsRemaining;
        this.hemoglobinLevel = hemoglobinLevel;
        this.screeningResult = screeningResult;
    }

    public int getDonationId() {
        return donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
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

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getUnits() {
        return units;
    }

    public int getUnitsRemaining() {
        return unitsRemaining;
    }

    public BigDecimal getHemoglobinLevel() {
        return hemoglobinLevel;
    }

    public String getScreeningResult() {
        return screeningResult;
    }

    @Override
    public String toString() {
        return "Donation #" + donationId + " (" + bloodGroup + ", " + units + " units, " + screeningResult + ")";
    }
}
