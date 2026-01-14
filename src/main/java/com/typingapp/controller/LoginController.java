package com.typingapp.controller;

import com.typingapp.database.DatabaseHelper;
import com.typingapp.model.User;
import com.typingapp.util.AlertHelper;
import com.typingapp.util.NavigationHelper;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * LoginController - Handles login and registration screen
 * 
 * This is the FIRST screen users see when opening the app.
 * 
 * Responsibilities:
 * - Handle user login
 * - Handle new user registration
 * - Provide guest play option (no account needed)
 * - Navigate to main menu after authentication
 */
public class LoginController {

    // ===== UI COMPONENTS (injected from FXML) =====
    @FXML
    private TextField usernameField; // Username input
    @FXML
    private PasswordField passwordField; // Password input (masked)

    private Stage stage; // Window reference for navigation

    /** Set stage reference from MainApp (needed for navigation) */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ===== LOGIN HANDLER =====
    /**
     * Handle login button click.
     * 
     * Steps:
     * 1. Get username and password from fields
     * 2. Validate: fields must not be empty
     * 3. Verify credentials with database
     * 4. If valid: navigate to menu
     * 5. If invalid: show error
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validate not empty
        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showInfo("Error", "Please fill in all fields");
            return;
        }

        // Verify with database
        User user = DatabaseHelper.loginUser(username, password);
        if (user != null) {
            goToMenu(user); // Login success
        } else {
            AlertHelper.showInfo("Login Failed", "Wrong username or password");
        }
    }

    // ===== REGISTER HANDLER =====
    /**
     * Handle register button click.
     * 
     * Steps:
     * 1. Get username and password
     * 2. Validate: not empty
     * 3. Validate: password min 4 chars
     * 4. Save to database
     * 5. Show success or error (username taken)
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showInfo("Error", "Please fill in all fields");
            return;
        }

        if (password.length() < 4) {
            AlertHelper.showInfo("Error", "Password must be at least 4 characters");
            return;
        }

        boolean success = DatabaseHelper.registerUser(username, password);
        if (success) {
            AlertHelper.showInfo("Success", "Account created successfully! Please login.");
            passwordField.clear();
        } else {
            AlertHelper.showInfo("Error", "Username already exists. Please choose another.");
        }
    }

    // ===== GUEST MODE HANDLER =====
    /** Play as guest - no login required, but scores won't be saved */
    @FXML
    private void handlePlayAsGuest() {
        goToMenu(null); // null user = guest
    }

    /** Navigate to main menu */
    private void goToMenu(User user) {
        try {
            NavigationHelper.goToMenu(user, stage);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load menu: " + e.getMessage());
        }
    }
}