package com.bloodbank.util;

import com.bloodbank.model.User;

/**
 * Singleton managing the authenticated session state across the application.
 */
public class Session {
    private static Session instance;
    private User currentUser;

    private Session() {
    }

    public static synchronized Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized void clear() {
        this.currentUser = null;
    }
}
