package com.typingapp.controller;

import com.typingapp.engine.GameEngine;
import com.typingapp.engine.TimeGame;
import com.typingapp.model.User;
import com.typingapp.util.AlertHelper;
import com.typingapp.util.Constants;
import com.typingapp.util.NavigationHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MenuController - Main menu screen controller
 * 
 * Responsibilities:
 * - Display game mode options (Time Mode / Custom)
 * - Manage language selection (Indonesia / English)
 * - Manage time duration selection (15s / 30s / 60s)
 * - Navigate to other screens (Game, Leaderboard, Profile)
 * - Display welcome message with username
 * 
 * Navigation from menu:
 * - Play Time Mode → GameController
 * - Play Custom → CustomController
 * - Leaderboard → LeaderboardController
 * - Profile → ProfileController
 * - Logout → LoginController
 */
public class MenuController {

    // ===== UI COMPONENTS (from FXML) =====
    @FXML
    private Label welcomeLabel; // Shows "Hello, [username]"
    @FXML
    private ComboBox<String> languageCombo; // Language dropdown
    @FXML
    private RadioButton rb15, rb30, rb60; // Time duration options
    @FXML
    private ToggleGroup timeGroup; // Group for radio buttons

    // ===== STATE VARIABLES =====
    private User currentUser; // Logged in user (null if guest)
    private Stage stage; // Window for navigation
    private int selectedTime = Constants.TIME_MODE_30; // Default: 30s
    private String selectedLanguage = Constants.LANG_INDONESIA; // Default: Indonesia

    // ===== INITIALIZATION =====
    /**
     * Called automatically by JavaFX after FXML is loaded.
     * Sets up initial UI state.
     */
    public void initialize() {
        // Setup radio buttons in toggle group (only one can be selected)
        timeGroup = new ToggleGroup();
        rb15.setToggleGroup(timeGroup);
        rb30.setToggleGroup(timeGroup);
        rb60.setToggleGroup(timeGroup);
        rb30.setSelected(true); // Default 30 seconds

        // Update selectedTime when radio button changes
        timeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == rb15)
                selectedTime = Constants.TIME_MODE_15;
            else if (newVal == rb30)
                selectedTime = Constants.TIME_MODE_30;
            else
                selectedTime = Constants.TIME_MODE_60;
        });

        // Setup language dropdown
        languageCombo.getItems().addAll(Constants.LANG_INDONESIA, Constants.LANG_ENGLISH);
        languageCombo.setValue(Constants.LANG_INDONESIA);
        languageCombo.setOnAction(e -> selectedLanguage = languageCombo.getValue());
    }

    /**
     * Receive user and stage from navigation.
     * Called by NavigationHelper after FXML loads.
     */
    public void setUserAndStage(User user, Stage stage) {
        this.currentUser = user;
        this.stage = stage;

        // Display welcome message
        String welcome = (currentUser != null)
                ? Constants.MSG_HELLO_PREFIX + currentUser.getUsername()
                : Constants.MSG_HELLO_PREFIX + Constants.MSG_GUEST;
        welcomeLabel.setText(welcome);
    }

    /**
     * Restore previous settings when returning from other screens.
     * 
     * BUG FIX: If time=0 (from Custom mode), default to 30s.
     * This prevents instant game end when switching from Custom to Time mode.
     */
    public void restoreSettings(String language, int time) {
        // Restore language (only if valid, not "Custom")
        if (language != null
                && (language.equals(Constants.LANG_INDONESIA) || language.equals(Constants.LANG_ENGLISH))) {
            languageCombo.setValue(language);
            this.selectedLanguage = language;
        } else {
            languageCombo.setValue(Constants.LANG_INDONESIA);
            this.selectedLanguage = Constants.LANG_INDONESIA;
        }

        // Restore time selection (fallback to 30s if invalid or 0)
        if (time == Constants.TIME_MODE_15) {
            rb15.setSelected(true);
            this.selectedTime = Constants.TIME_MODE_15;
        } else if (time == Constants.TIME_MODE_60) {
            rb60.setSelected(true);
            this.selectedTime = Constants.TIME_MODE_60;
        } else {
            // Default to 30s for any invalid value (including 0 from Custom mode)
            rb30.setSelected(true);
            this.selectedTime = Constants.TIME_MODE_30;
        }
    }

    // ===== BUTTON HANDLERS =====

    /** Start timed game with TimeGame engine */
    @FXML
    private void handlePlayTimeMode() {
        GameEngine engine = new TimeGame();
        String filename = selectedLanguage.equals(Constants.LANG_INDONESIA) ? "words_id.txt" : "words_en.txt";
        List<String> words = loadWords(filename);
        startGame(engine, words, false);
    }

    /** Navigate to custom text input screen */
    @FXML
    private void handlePlayCustom() {
        try {
            NavigationHelper.goToCustom(currentUser, stage, selectedLanguage, selectedTime);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load custom mode: " + e.getMessage());
        }
    }

    /** Navigate to leaderboard */
    @FXML
    private void handleLeaderboard() {
        try {
            NavigationHelper.goToLeaderboard(currentUser, stage, selectedLanguage, selectedTime);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load leaderboard: " + e.getMessage());
        }
    }

    /** Navigate to profile (requires login) */
    @FXML
    private void handleProfile() {
        if (currentUser == null) {
            AlertHelper.showInfo("Error", "Please login first");
            return;
        }

        try {
            NavigationHelper.goToProfile(currentUser, stage, selectedLanguage, selectedTime);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load profile: " + e.getMessage());
        }
    }

    /** Navigate back to login */
    @FXML
    private void handleLogout() {
        try {
            NavigationHelper.goToLogin(stage);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to logout: " + e.getMessage());
        }
    }

    // ===== UTILITY METHODS =====

    /** Start game with given engine and words */
    private void startGame(GameEngine engine, List<String> words, boolean isCustom) {
        try {
            NavigationHelper.goToGame(engine, words, isCustom, selectedTime, currentUser, stage, selectedLanguage);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load game: " + e.getMessage());
        }
    }

    /**
     * Load words from resource file.
     * - Reads file from /data/ folder
     * - Shuffles words
     * - Duplicates if needed to fill MAX_WORDS_POOL
     */
    private List<String> loadWords(String filename) {
        List<String> list = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("/data/" + filename);

        if (is == null) {
            System.err.println("Could not find word file: /data/" + filename);
            list.add("error");
            list.add("missing_file");
            return list;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                Collections.addAll(list, parts);
            }
        } catch (Exception e) {
            e.printStackTrace();
            list.add("error");
            list.add("loading");
            list.add("file");
        }

        Collections.shuffle(list);

        // Duplicate words to fill pool if needed
        List<String> result = new ArrayList<>();
        while (result.size() < Constants.MAX_WORDS_POOL) {
            result.addAll(list.stream().limit(100).collect(Collectors.toList()));
        }

        return result.stream().limit(Constants.MAX_WORDS_POOL).collect(Collectors.toList());
    }
}