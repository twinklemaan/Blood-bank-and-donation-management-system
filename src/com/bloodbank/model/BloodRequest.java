package com.bloodbank.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;

/**
 * Model representing a hospital's blood request.
 * Contains Comparator for sorting requests by urgency then date (Rubric: Collections).
 */
public class BloodRequest {
    private final int requestId;
    private final int hospitalId;
    private String hospitalName;
    private final int bankId;
    private String bankName;
    private final String bloodGroup;
    private final int unitsRequested;
    private final String urgency; // Critical, Urgent, Normal
    private String status;        // Pending, Fulfilled, Rejected
    private final LocalDateTime requestDate;
    private LocalDateTime fulfilledDate;

    // Urgency weight map for prioritization: lower number = higher priority
    private static final Map<String, Integer> URGENCY_PRIORITY = Map.of(
            "Critical", 1,
            "Urgent", 2,
            "Normal", 3
    );

    /**
     * Rubric: Collections requirement - Comparator sorting requests by urgency then date.
     * Critical -> Urgent -> Normal, then oldest request date first.
     */
    public static final Comparator<BloodRequest> BY_URGENCY_THEN_DATE = (r1, r2) -> {
        int p1 = URGENCY_PRIORITY.getOrDefault(r1.getUrgency(), 4);
        int p2 = URGENCY_PRIORITY.getOrDefault(r2.getUrgency(), 4);
        int urgencyComp = Integer.compare(p1, p2);
        if (urgencyComp != 0) {
            return urgencyComp;
        }
        if (r1.getRequestDate() == null && r2.getRequestDate() == null) {
            return 0;
        }
        if (r1.getRequestDate() == null) {
            return 1;
        }
        if (r2.getRequestDate() == null) {
            return -1;
        }
        return r1.getRequestDate().compareTo(r2.getRequestDate());
    };

    public BloodRequest(int requestId, int hospitalId, String hospitalName, int bankId, String bankName,
                        String bloodGroup, int unitsRequested, String urgency, String status,
                        LocalDateTime requestDate, LocalDateTime fulfilledDate) {
        this.requestId = requestId;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.bankId = bankId;
        this.bankName = bankName;
        this.bloodGroup = bloodGroup;
        this.unitsRequested = unitsRequested;
        this.urgency = urgency;
        this.status = status;
        this.requestDate = requestDate;
        this.fulfilledDate = fulfilledDate;
    }

    public int getRequestId() {
        return requestId;
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

    public int getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public int getUnitsRequested() {
        return unitsRequested;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public LocalDateTime getFulfilledDate() {
        return fulfilledDate;
    }

    public void setFulfilledDate(LocalDateTime fulfilledDate) {
        this.fulfilledDate = fulfilledDate;
    }

    @Override
    public String toString() {
        return "Request #" + requestId + " [" + urgency + "] " + bloodGroup + " (" + unitsRequested + " units) - " + status;
    }
}
