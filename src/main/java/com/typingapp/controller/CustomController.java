package com.typingapp.controller;

import com.typingapp.engine.CustomGame;
import com.typingapp.engine.GameEngine;
import com.typingapp.model.User;
import com.typingapp.util.AlertHelper;
import com.typingapp.util.Constants;
import com.typingapp.util.NavigationHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CustomController - Custom text input screen controller
 * 
 * Allows users to enter their own text for typing practice.
 * 
 * Responsibilities:
 * - Provide TextArea for custom text input
 * - Display real-time word count
 * - Validate minimum words before starting
 * - Start game with custom text
 * 
 * Differences from Time Mode:
 * - Text from user input (not from file)
 * - Word order is NOT shuffled
 * - No time limit (infinite)
 * - Score is NOT saved to database
 */
public class CustomController {

    // ===== UI COMPONENTS (from FXML) =====
    @FXML
    private TextArea customTextArea; // Custom text input area
    @FXML
    private Label wordCountLabel; // Word count display

    // ===== STATE VARIABLES =====
    private User currentUser;
    private Stage stage;
    private String prevLanguage; // For restoring menu settings
    private int prevTime;

    /**
     * Called automatically after FXML loads.
     * Sets up listener for real-time word count updates.
     */
    public void initialize() {
        customTextArea.textProperty().addListener((obs, oldVal, newVal) -> updateWordCount());
    }

    /** Set user and stage from navigation */
    public void setUserAndStage(User user, Stage stage) {
        this.currentUser = user;
        this.stage = stage;
    }

    /** Save menu state for restoration when returning */
    public void setReturnState(String prevLanguage, int prevTime) {
        this.prevLanguage = prevLanguage;
        this.prevTime = prevTime;
    }

    /** Update word count label (called on every text change) */
    private void updateWordCount() {
        String text = customTextArea.getText().trim();
        if (text.isEmpty()) {
            wordCountLabel.setText("Words: 0");
        } else {
            String[] words = text.split("\\s+");
            wordCountLabel.setText("Words: " + words.length);
        }
    }

    // ===== START GAME HANDLER =====
    /**
     * Handle "Start Game" button.
     * Validates input and starts game with custom text.
     * 
     * Steps:
     * 1. Get text from TextArea
     * 2. Validate: cannot be empty
     * 3. Parse text into word list (NO shuffle!)
     * 4. Validate: minimum word count
     * 5. Create CustomGame engine
     * 6. Navigate to GameController
     */
    @FXML
    private void handleStartGame() {
        String text = customTextArea.getText().trim();

        if (text.isEmpty()) {
            AlertHelper.showInfo("Error", "Please enter some text first!");
            return;
        }

        // Parse into word list (IMPORTANT: not shuffled)
        List<String> words = Arrays.stream(text.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());

        if (words.size() < Constants.MIN_WORDS_FOR_CUSTOM) {
            AlertHelper.showInfo("Error", "Please enter at least " + Constants.MIN_WORDS_FOR_CUSTOM + " words!");
            return;
        }

        GameEngine engine = new CustomGame();
        startGame(engine, words);
    }

    /** Clear the text area */
    @FXML
    private void handleClear() {
        customTextArea.clear();
        customTextArea.requestFocus();
    }

    /** Back to menu */
    @FXML
    private void handleBackToMenu() {
        goToMenu();
    }

    // ===== UTILITY METHODS =====

    /**
     * Start game with custom engine and words.
     * Custom mode: isCustom=true, time=0 (infinite), language="Custom"
     * 
     * PASS restoreTime = prevTime so GameController knows what time setting
     * to use when user goes back to menu.
     */
    private void startGame(GameEngine engine, List<String> words) {
        try {
            // Pass prevTime as the last argument (restoreTime)
            NavigationHelper.goToGame(engine, words, true, 0, currentUser, stage, Constants.LANG_CUSTOM, prevTime);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load game: " + e.getMessage());
        }
    }

    /** Navigate back to menu with preserved settings */
    private void goToMenu() {
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
}
