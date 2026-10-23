package com.bloodbank.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Concrete class representing a blood donor.
 * Extends Person (Rubric: Inheritance).
 */
public class Donor extends Person {
    private final String gender;
    private final LocalDate dob;
    private final String bloodGroup;
    private Integer age;
    private LocalDate nextEligibleDate;

    public Donor(int id, String name, String gender, LocalDate dob, String bloodGroup,
                 String city, String state, String pincode, List<String> phones) {
        super(id, name, city, state, pincode, phones);
        this.gender = gender;
        this.dob = dob;
        this.bloodGroup = bloodGroup;
    }

    public Donor(int id, String name, String gender, LocalDate dob, String bloodGroup,
                 String city, String state, String pincode, List<String> phones,
                 Integer age, LocalDate nextEligibleDate) {
        super(id, name, city, state, pincode, phones);
        this.gender = gender;
        this.dob = dob;
        this.bloodGroup = bloodGroup;
        this.age = age;
        this.nextEligibleDate = nextEligibleDate;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getNextEligibleDate() {
        return nextEligibleDate;
    }

    public void setNextEligibleDate(LocalDate nextEligibleDate) {
        this.nextEligibleDate = nextEligibleDate;
    }

    @Override
    public String toString() {
        return getName() + " [" + bloodGroup + "] (ID: " + getId() + ")";
    }
}
