package com.bloodbank.model;

import java.util.List;

/**
 * Abstract base class representing an authenticated user in the system.
 * Demonstrates inheritance and polymorphism via getTabs() (Rubric: Inheritance).
 */
public abstract class User {
    private final int userId;
    private final String username;
    private final String role;

    public User(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    /**
     * Polymorphic method overridden by subclasses to define the UI tabs available to the user.
     */
    public abstract List<String> getTabs();

    public abstract String getRoleDisplayName();

    @Override
    public String toString() {
        return username + " [" + getRoleDisplayName() + "]";
    }
}
