package com.typingapp.engine;

import com.typingapp.model.User;
import com.typingapp.database.DatabaseHelper;

/**
 * TimeGame - Timed game mode (Child Class)
 * 
 * Extends GameEngine with specific behavior for timed games.
 * 
 * OOP CONCEPTS:
 * - INHERITANCE: Inherits all logic from GameEngine (WPM, Accuracy calculation)
 * - POLYMORPHISM: Overrides endGame() to save scores to database
 * 
 * Available time modes: 15s, 30s, 60s
 */
public class TimeGame extends GameEngine {

    /**
     * End game and SAVE score to database.
     * 
     * POLYMORPHISM: Unlike CustomGame, TimeGame saves to database.
     * 
     * Steps:
     * 1. Stop game (isRunning = false)
     * 2. Check if user is valid (not guest)
     * 3. Calculate all metrics
     * 4. Save to database
     */
    @Override
    public void endGame(User user, int timeSetting, String language) {
        setRunning(false);

        // Only save if user is logged in (not guest)
        if (user != null && user.getId() > 0) {
            long elapsed = System.currentTimeMillis() - startTime;

            // Calculate all metrics using parent class methods
            int netWPM = calculateNetWPM(elapsed);
            int grossWPM = calculateGrossWPM(elapsed);
            double accuracy = calculateAccuracy();
            double weightedScore = calculateWeightedScore(elapsed);

            // Save to database
            DatabaseHelper.addScore(user.getId(), netWPM, grossWPM, accuracy, weightedScore, timeSetting, language);
        }
    }

    /**
     * Process input with special space validation.
     * 
     * POLYMORPHISM: Overrides parent to add stricter space handling.
     * 
     * Space rules:
     * - Typing SPACE when target is NOT space → WRONG
     * - Typing NON-SPACE when target is space → WRONG
     * 
     * KEYSTROKE TRACKING: Same as parent - every keystroke counts for accuracy
     */
    @Override
    public void processInput(char input, char target) {
        if (!isRunning())
            return;

        boolean isCorrect = (input == target);

        // Special validation for SPACE character
        if (input == ' ' && target != ' ') {
            isCorrect = false; // Pressed space when should type letter
        } else if (input != ' ' && target == ' ') {
            isCorrect = false; // Typed letter when should press space
        }

        // Track keystroke for accuracy (PERMANENT - backspace won't undo)
        totalKeystrokes++;
        if (isCorrect) {
            correctKeystrokes++;
        }

        // Save status for UI highlighting
        if (currentIndex < charStatus.size()) {
            charStatus.set(currentIndex, isCorrect);
        }

        // Update display stats (can be reversed by backspace)
        if (isCorrect) {
            currentCorrectChars++;
        } else {
            currentErrors++;
        }

        currentIndex++;
    }
}