package com.typingapp.engine;

import com.typingapp.model.User;

/**
 * CustomGame - Custom text game mode (Child Class)
 * 
 * Extends GameEngine for sandbox/practice mode with user-provided text.
 * 
 * OOP CONCEPTS:
 * - INHERITANCE: Inherits all calculation logic from GameEngine
 * - POLYMORPHISM: Overrides endGame() to NOT save scores
 * 
 * Characteristics:
 * - No time limit (infinite)
 * - Text provided by user (not shuffled)
 * - Score NOT saved to leaderboard
 * - Game ends when all words are typed
 */
public class CustomGame extends GameEngine {

    /**
     * End game WITHOUT saving to database.
     * 
     * POLYMORPHISM: Unlike TimeGame, CustomGame does NOT save scores.
     * This is "Sandbox Mode" for free practice.
     * 
     * Why not save:
     * - Mode is for practice/experimentation
     * - Users could input easy text to cheat
     * - Leaderboard should be fair with standard text
     */
    @Override
    public void endGame(User user, int timeSetting, String language) {
        setRunning(false);

        // Just log to console, don't save to database
        System.out.println("Custom Game Finished. Score not saved (Sandbox Mode). Language: " + language);
    }
}