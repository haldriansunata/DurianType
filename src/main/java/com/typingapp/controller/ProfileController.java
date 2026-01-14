package com.typingapp.controller;

import com.typingapp.database.DatabaseHelper;
import com.typingapp.model.Score;
import com.typingapp.model.User;
import com.typingapp.util.AlertHelper;
import com.typingapp.util.Constants;
import com.typingapp.util.NavigationHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * ProfileController - User profile screen controller
 * 
 * Responsibilities:
 * - Display user account info
 * - Allow updating username and password
 * - Display game history (score records)
 * - Handle account deletion
 * 
 * Features:
 * - Update profile (username and/or password)
 * - Filter history by language
 * - Delete account with confirmation
 */
public class ProfileController {

    // ===== UI COMPONENTS - ACCOUNT INFO =====
    @FXML
    private Label usernameLabel; // Shows current username
    @FXML
    private TextField newUsernameField; // New username input
    @FXML
    private PasswordField newPasswordField; // New password input
    @FXML
    private PasswordField confirmPasswordField; // Confirm password input

    // ===== UI COMPONENTS - HISTORY FILTER =====
    @FXML
    private ComboBox<String> historyLanguageCombo; // Language filter dropdown

    // ===== UI COMPONENTS - HISTORY TABLE =====
    @FXML
    private TableView<Score> historyTable;
    @FXML
    private TableColumn<Score, String> modeColumn; // Mode (15s/30s/60s)
    @FXML
    private TableColumn<Score, String> languageColumn; // Language (ID/EN)
    @FXML
    private TableColumn<Score, Integer> wpmColumn; // WPM
    @FXML
    private TableColumn<Score, String> accuracyColumn; // Accuracy
    @FXML
    private TableColumn<Score, String> dateColumn; // Date

    // ===== STATE VARIABLES =====
    private User currentUser;
    private Stage stage;
    private String prevLanguage; // For restoring menu settings
    private int prevTime;

    /**
     * Called automatically after FXML loads.
     * Sets up table columns and filter.
     */
    public void initialize() {
        // Mode column: format as "15s/30s/60s" or "Custom"
        modeColumn.setCellValueFactory(cellData -> {
            int timeMode = cellData.getValue().getTimeMode();
            String mode = timeMode > 0 ? timeMode + "s" : Constants.LANG_CUSTOM;
            return new javafx.beans.property.SimpleStringProperty(mode);
        });

        // Bind other columns to Score properties
        languageColumn.setCellValueFactory(new PropertyValueFactory<>("language"));
        wpmColumn.setCellValueFactory(new PropertyValueFactory<>("wpm"));
        accuracyColumn.setCellValueFactory(new PropertyValueFactory<>("accuracyFormatted"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));

        // Setup language filter
        historyLanguageCombo.getItems().addAll("All", "English", "Indonesia");
        historyLanguageCombo.setValue("All");
        historyLanguageCombo.setOnAction(e -> loadHistory());
    }

    /** Set user and stage from navigation */
    public void setUserAndStage(User user, Stage stage) {
        this.currentUser = user;
        this.stage = stage;

        if (currentUser != null) {
            usernameLabel.setText("Username: " + currentUser.getUsername());
            newUsernameField.setText(currentUser.getUsername());
            loadHistory();
        }
    }

    /** Save menu state for restoration when returning */
    public void setReturnState(String prevLanguage, int prevTime) {
        this.prevLanguage = prevLanguage;
        this.prevTime = prevTime;
    }

    /** Load game history from database with language filter */
    private void loadHistory() {
        if (currentUser == null)
            return;

        String languageFilter = historyLanguageCombo.getValue();
        List<Score> history = DatabaseHelper.getHistory(currentUser.getId(), languageFilter);
        historyTable.setItems(FXCollections.observableArrayList(history));
    }

    // ===== UPDATE PROFILE HANDLER =====
    /**
     * Handle profile update.
     * User can change username only, password only, or both.
     * 
     * Validation:
     * 1. Username cannot be empty
     * 2. If changing password: min 4 chars
     * 3. If changing password: must match confirmation
     */
    @FXML
    private void handleUpdateProfile() {
        String newUsername = newUsernameField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Username required
        if (newUsername.isEmpty()) {
            AlertHelper.showInfo("Error", "Username cannot be empty!");
            return;
        }

        // Check if changing password
        boolean changingPassword = !newPassword.isEmpty() || !confirmPassword.isEmpty();

        if (changingPassword) {
            if (newPassword.length() < Constants.MIN_PASSWORD_LENGTH) {
                AlertHelper.showInfo("Error",
                        "Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                AlertHelper.showInfo("Error", "Passwords do not match!");
                return;
            }
        }

        // Use placeholder if not changing password
        String passwordToUse = changingPassword ? newPassword : Constants.PASSWORD_PLACEHOLDER;

        boolean success = DatabaseHelper.updateUser(currentUser.getId(), newUsername, passwordToUse);

        if (success) {
            currentUser.setUsername(newUsername);
            usernameLabel.setText("Username: " + newUsername);
            AlertHelper.showInfo("Success", "Profile updated successfully!");
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            AlertHelper.showInfo("Error", "Failed to update profile. Username might be taken.");
        }
    }

    // ===== DELETE ACCOUNT HANDLER =====
    /**
     * Handle account deletion.
     * Shows confirmation dialog before deleting.
     * WARNING: This cannot be undone!
     */
    @FXML
    private void handleDeleteAccount() {
        boolean confirmed = AlertHelper.showConfirmation(
                "Delete Account",
                "Are you sure you want to delete your account?",
                "This action cannot be undone. All your data will be permanently deleted.");

        if (confirmed) {
            DatabaseHelper.deleteUser(currentUser.getId());
            AlertHelper.showInfo("Account Deleted", "Your account has been deleted successfully.");
            goToLogin();
        }
    }

    // ===== NAVIGATION =====

    /** Back to menu with preserved settings */
    @FXML
    private void handleBackToMenu() {
        try {
            if (prevLanguage != null && prevTime > 0) {
                NavigationHelper.goToMenu(currentUser, stage, prevLanguage, prevTime);
            } else {
                NavigationHelper.goToMenu(currentUser, stage);
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load menu: " + e.getMessage());
        }
    }

    /** Navigate to login (after delete account) */
    private void goToLogin() {
        try {
            NavigationHelper.goToLogin(stage);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load login: " + e.getMessage());
        }
    }

    /** Logout and return to login */
    @FXML
    private void handleLogout() {
        try {
            NavigationHelper.goToLogin(stage);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to logout: " + e.getMessage());
        }
    }
}
