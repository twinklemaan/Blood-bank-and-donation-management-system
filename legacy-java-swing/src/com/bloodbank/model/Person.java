package com.bloodbank.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class representing an individual person in the system.
 * Demonstrates inheritance and defensive collection encapsulation.
 */
public abstract class Person {
    private final int id;
    private final String name;
    private final String city;
    private final String state;
    private final String pincode;
    private final List<String> phones = new ArrayList<>();

    public Person(int id, String name, String city, String state, String pincode, List<String> phones) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        if (phones != null) {
            this.phones.addAll(phones);
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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

    /**
     * Returns an unmodifiable list of phone numbers (Rubric: Collections requirement).
     */
    public List<String> getPhones() {
        return Collections.unmodifiableList(phones);
    }

    public String getPrimaryPhone() {
        return phones.isEmpty() ? "" : phones.get(0);
    }

    @Override
    public String toString() {
        return name + " (" + city + ")";
    }
}
