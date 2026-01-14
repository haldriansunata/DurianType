package com.typingapp.controller;

import com.typingapp.engine.GameEngine;
import com.typingapp.model.User;
import com.typingapp.util.AlertHelper;
import com.typingapp.util.Constants;
import com.typingapp.util.NavigationHelper;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GameController - Main typing game screen controller
 * 
 * Responsibilities:
 * - Display words to type
 * - Capture keyboard input
 * - Update stats display (WPM, Accuracy, Timer)
 * - Manage word batch changes (infinite scroll)
 * - End game and navigate back to menu
 * 
 * Relationship with GameEngine:
 * - Controller handles UI/UX
 * - Engine handles logic & calculations
 * - Controller calls Engine methods for each input
 */
public class GameController {

    // ===== UI COMPONENTS (from FXML) =====
    @FXML
    private Label timerLabel; // Shows remaining time or "∞"
    @FXML
    private Label wpmLabel; // Shows WPM
    @FXML
    private Label accuracyLabel; // Shows accuracy percentage
    @FXML
    private TextFlow textDisplay; // Container for colored characters
    @FXML
    private VBox gameContainer; // Main container (for focus)
    @FXML
    private Button btnGiveUp; // Give up button
    @FXML
    private TextField hiddenInputField; // Hidden field for keyboard capture

    @FXML
    private ScrollPane gameScrollPane; // ScrollPane for text display

    // ===== ENGINE & DATA =====
    private GameEngine gameEngine; // Reference to engine (TimeGame or CustomGame)
    private List<String> allWordsPool; // All available words
    private String fullText; // Current text being displayed
    private boolean isCustomMode; // Custom mode vs time mode flag
    private int selectedTime; // Game duration (15/30/60, 0 for custom)
    private int timeToRestore; // Time to restore when returning to menu (for Custom mode)
    private User currentUser; // Current player (null if guest)
    private long startTime; // Timestamp when game started
    private AnimationTimer gameTimer; // Timer for periodic UI updates
    private Stage stage; // Window for navigation
    private String selectedLanguage; // Language of words

    // ===== BATCH STATE =====
    private int currentWordIndex = 0; // Index in word pool (for custom mode)
    private boolean isGameActive = false; // Has user started typing?

    /**
     * Initialize controller.
     * Called automatically after FXML is loaded.
     */
    public void initialize() {
        // CRITICAL: Bind TextFlow maxWidth (NOT prefWidth!) to ScrollPane width
        // TextFlow in JavaFX ONLY wraps text when maxWidth is constrained
        // prefWidth does NOT trigger wrapping behavior
        textDisplay.maxWidthProperty().bind(gameScrollPane.widthProperty().subtract(60));
    }

    /**
     * Initialize game with all required data.
     * Called by NavigationHelper after FXML loads.
     * 
     * Steps:
     * 1. Store all parameters
     * 2. Shuffle words (only for time mode)
     * 3. Get first batch of words
     * 4. Start game in engine
     * 5. Render initial display
     * 
     * @param restoreTime Time to restore when returning to menu (for Custom mode)
     */
    public void setGameData(GameEngine engine, List<String> words, boolean isCustom, int time, User user, Stage stage,
            String language, int restoreTime) {
        this.gameEngine = engine;
        this.allWordsPool = new ArrayList<>(words);
        this.isCustomMode = isCustom;
        this.selectedTime = time;
        this.timeToRestore = restoreTime; // Time to restore when going back to menu
        this.currentUser = user;
        this.stage = stage;
        this.selectedLanguage = language;
        this.currentWordIndex = 0;
        this.isGameActive = false;

        // Shuffle only for time mode (custom keeps original order)
        if (!isCustomMode) {
            Collections.shuffle(allWordsPool);
        }

        // Get first batch and start
        List<String> firstBatch = getNextBatch();
        updateFullText(firstBatch);
        gameEngine.startGame(firstBatch);

        // Render initial display
        updateTimerDisplay();
        renderText();

        // Focus container for keyboard events
        Platform.runLater(() -> gameContainer.requestFocus());
    }

    // ===== BATCH MANAGEMENT (INFINITE SCROLL) =====

    /**
     * Get next batch of words from pool.
     * 
     * CUSTOM MODE: Takes words sequentially, returns empty when done
     * TIME MODE: Shuffles and takes from start (infinite)
     */
    private List<String> getNextBatch() {
        if (isCustomMode) {
            int endIndex = Math.min(currentWordIndex + Constants.BATCH_SIZE, allWordsPool.size());
            if (currentWordIndex >= allWordsPool.size()) {
                return new ArrayList<>(); // No more words
            }
            List<String> batch = allWordsPool.subList(currentWordIndex, endIndex);
            currentWordIndex = endIndex;
            return new ArrayList<>(batch);
        } else {
            Collections.shuffle(allWordsPool);
            return allWordsPool.subList(0, Math.min(allWordsPool.size(), Constants.BATCH_SIZE));
        }
    }

    /** Convert word list to single string with spaces */
    private void updateFullText(List<String> batchWords) {
        StringBuilder sb = new StringBuilder();
        for (String word : batchWords) {
            sb.append(word).append(" ");
        }
        this.fullText = sb.toString();
    }

    // ===== KEYBOARD INPUT HANDLER =====

    /**
     * Handle each key press.
     * 
     * Flow:
     * 1. Validate: game must be running
     * 2. Validate: must be printable character
     * 3. Start timer on first keystroke
     * 4. Check if batch needs refresh
     * 5. Process input in engine
     * 6. Update display
     */
    @FXML
    private void handleKeyTyped(KeyEvent event) {
        if (!gameEngine.isRunning())
            return;

        String character = event.getCharacter();
        if (character.isEmpty())
            return;

        char typedChar = character.charAt(0);

        // Handle BACKSPACE (char code 8)
        if (typedChar == 8) {
            if (gameEngine.processBackspace()) {
                renderText();
            }
            return;
        }

        // Ignore other control characters (Tab, Enter, Escape)
        if (typedChar == 9 || typedChar == 13 || typedChar == 27)
            return;

        // Only accept printable ASCII (32-126)
        if (typedChar < 32 || typedChar > 126)
            return;

        // Start timer on first keystroke
        if (!isGameActive) {
            isGameActive = true;
            startTime = System.currentTimeMillis();
            gameEngine.resetStartTime();
            startGameTimer();
        }

        // Check if reached end of batch
        if (gameEngine.getCurrentIndex() >= fullText.length()) {
            if (isCustomMode && currentWordIndex >= allWordsPool.size()) {
                finishGame();
                return;
            }
            refreshWordsBatch();
            return;
        }

        // Process input in engine
        char targetChar = fullText.charAt(gameEngine.getCurrentIndex());
        gameEngine.processInput(typedChar, targetChar);

        // Check again after processing
        if (gameEngine.getCurrentIndex() >= fullText.length()) {
            if (isCustomMode && currentWordIndex >= allWordsPool.size()) {
                finishGame();
                return;
            }
            refreshWordsBatch();
        } else {
            renderText();
        }

        updateStats();
    }

    /** Load new batch and update display */
    private void refreshWordsBatch() {
        List<String> nextBatch = getNextBatch();
        if (nextBatch.isEmpty() && isCustomMode) {
            finishGame();
            return;
        }
        updateFullText(nextBatch);
        gameEngine.loadNewBatch(nextBatch);
        renderText();
    }

    // ===== TEXT RENDERING (CHARACTER HIGHLIGHTING) =====

    /**
     * Render words with different colors per character.
     * 
     * Colors:
     * - GREEN (char-correct): typed correctly
     * - RED (char-wrong): typed incorrectly
     * - UNDERLINE (char-current): current target
     * - GRAY (char-default): not typed yet
     */
    private void renderText() {
        textDisplay.getChildren().clear();

        int currentIndex = gameEngine.getCurrentIndex();
        List<Boolean> charStatus = gameEngine.getCharStatus();

        for (int i = 0; i < fullText.length(); i++) {
            char c = fullText.charAt(i);
            Text textNode;

            // Special: wrong space → show as red underscore
            if (i < currentIndex && c == ' ') {
                Boolean correct = (i < charStatus.size()) ? charStatus.get(i) : false;
                if (correct != null && !correct) {
                    textNode = new Text("_");
                    textNode.getStyleClass().add("space-error");
                    textDisplay.getChildren().add(textNode);
                    continue;
                }
            }

            textNode = new Text(String.valueOf(c));
            textNode.getStyleClass().clear();

            // Determine color based on position and status
            if (i < currentIndex) {
                Boolean correct = (i < charStatus.size()) ? charStatus.get(i) : false;
                if (correct != null && correct) {
                    textNode.getStyleClass().add("char-correct");
                } else {
                    textNode.getStyleClass().add("char-wrong");
                }
            } else if (i == currentIndex) {
                textNode.getStyleClass().add("char-current");
            } else {
                textNode.getStyleClass().add("char-default");
            }

            textDisplay.getChildren().add(textNode);
        }
    }

    // ===== GAME TIMER =====

    /**
     * Start timer for periodic UI updates.
     * Uses AnimationTimer running every frame (~60fps).
     */
    private void startGameTimer() {
        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameEngine.isRunning()) {
                    stop();
                    return;
                }

                long elapsed = System.currentTimeMillis() - startTime;

                // Time mode: check if time is up
                if (!isCustomMode) {
                    long remaining = (selectedTime * 1000L) - elapsed;
                    if (remaining <= 0) {
                        finishGame();
                        stop();
                        return;
                    }
                    updateTimerDisplay(remaining / 1000);
                }

                updateStats();
            }
        };
        gameTimer.start();
    }

    // ===== DISPLAY UPDATES =====

    /** Set initial timer display */
    private void updateTimerDisplay() {
        timerLabel.setText(isCustomMode ? "∞" : String.valueOf(selectedTime));
    }

    /** Update timer with remaining seconds */
    private void updateTimerDisplay(long seconds) {
        timerLabel.setText(seconds + "s");
    }

    /** Update WPM and Accuracy display */
    private void updateStats() {
        long elapsed = System.currentTimeMillis() - startTime;
        int netWpm = gameEngine.calculateNetWPM(elapsed);
        double accuracy = gameEngine.calculateAccuracy();

        wpmLabel.setText(String.valueOf(netWpm));
        accuracyLabel.setText(String.format("%.1f%%", accuracy));
    }

    // ===== GAME FINISH =====

    /**
     * End game and show results.
     * 
     * Steps:
     * 1. Stop timer
     * 2. Call engine.endGame() (saves to DB if TimeGame)
     * 3. Show results dialog
     * 4. Navigate back to menu
     */
    private void finishGame() {
        if (gameTimer != null)
            gameTimer.stop();
        gameEngine.endGame(currentUser, selectedTime, selectedLanguage);

        Platform.runLater(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            int netWpm = gameEngine.calculateNetWPM(elapsed);
            int grossWpm = gameEngine.calculateGrossWPM(elapsed);
            double accuracy = gameEngine.calculateAccuracy();
            double weightedScore = gameEngine.calculateWeightedScore(elapsed);

            AlertHelper.showGameOverEnhanced(isCustomMode, netWpm, grossWpm, accuracy, weightedScore);
            backToMenu();
        });
    }

    // ===== NAVIGATION =====

    /** Handle Give Up button - stop game and return to menu */
    @FXML
    private void handleGiveUp() {
        if (gameTimer != null)
            gameTimer.stop();
        gameEngine.stopGame();
        backToMenu();
    }

    /** Navigate back to menu with settings preserved */
    private void backToMenu() {
        try {
            // Logic:
            // - If playing TimeMode (time > 0), restore that time.
            // - If playing CustomMode (time == 0), restore the saved timeToRestore.
            int timeSetting = (selectedTime > 0) ? selectedTime : this.timeToRestore;

            // Safety fallback if even timeToRestore is 0 (shouldn't happen if passed
            // correctly)
            if (timeSetting == 0)
                timeSetting = Constants.TIME_MODE_30;

            NavigationHelper.goToMenuWithSettings(currentUser, stage, selectedLanguage, timeSetting);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Failed to load menu: " + e.getMessage());
        }
    }
}