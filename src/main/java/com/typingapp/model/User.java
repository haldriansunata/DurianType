package com.typingapp.model;

/**
 * User - Represents a user account
 * 
 * Uses ENCAPSULATION (OOP concept):
 * - Private attributes (id, username)
 * - Public getters to access data
 * - Setter only for modifiable field (username)
 * 
 * Used for:
 * - Storing logged-in user info
 * - Identifying score ownership
 * - Personalizing UI (welcome message)
 */
public class User {

    private int id; // Primary key from database (auto-increment)
    private String username; // Unique username for login

    /**
     * Creates a User object.
     * Called when fetching user data from database after login.
     */
    public User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    /** Gets user ID (used as foreign key in scores table) */
    public int getId() {
        return id;
    }

    /** Gets username (displayed in UI and leaderboard) */
    public String getUsername() {
        return username;
    }

    /** Updates username (called when user edits profile) */
    public void setUsername(String username) {
        this.username = username;
    }
}
