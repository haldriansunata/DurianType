package com.typingapp.engine;

import com.typingapp.model.User;
import com.typingapp.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * GameEngine - Core game logic (Abstract Parent Class)
 * 
 * This is the "brain" of the typing game. It handles:
 * - Input validation (correct/wrong)
 * - Score calculation (WPM, Accuracy, Weighted Score)
 * - Game state management
 * 
 * OOP CONCEPTS USED:
 * - ABSTRACTION: Can't be instantiated directly, must be extended
 * - INHERITANCE: TimeGame and CustomGame inherit all logic
 * - POLYMORPHISM: endGame() is overridden with different behavior in child
 * classes
 * 
 * Workflow:
 * 1. Controller calls startGame() with word list
 * 2. Each keystroke is processed by processInput()
 * 3. Stats (WPM, Accuracy) calculated in real-time
 * 4. When finished, endGame() is called (behavior depends on child class)
 */
public abstract class GameEngine {

    // ===== WORD TRACKING =====
    protected List<String> targetWords; // Words the user needs to type
    protected List<Boolean> charStatus; // Status per char: null=not typed, true=correct, false=wrong
    protected int currentIndex; // Current cursor position (which char is targeted)

    // ===== CURRENT BATCH STATS =====
    protected int currentErrors; // Errors in current batch (for display)
    protected int currentCorrectChars; // Correct chars in current batch (for display)

    // ===== KEYSTROKE TRACKING (for accuracy calculation) =====
    // ACCURACY APPROACH: Like Monkeytype/10FastFingers
    // - Every keystroke is counted, including errors
    // - Backspace allows correction, but error still counts toward total
    // - Formula: Accuracy = (correctKeystrokes / totalKeystrokes) × 100
    // - This means: even if you fix an error, it still lowers your accuracy
    protected int totalKeystrokes; // Every key press (not backspace)
    protected int correctKeystrokes; // Only keys that were correct

    // ===== ACCUMULATED HISTORY (for infinite scroll) =====
    // Stores totals from previous batches so score isn't lost on page change
    protected int historyErrors;
    protected int historyCorrectChars;
    protected int historyTotalKeystrokes; // History of total keystrokes
    protected int historyCorrectKeystrokes; // History of correct keystrokes

    // ===== GAME STATE =====
    protected long startTime; // Timestamp when game started (for WPM calculation)
    private boolean isRunning; // Is game currently active?

    public GameEngine() {
        this.targetWords = new ArrayList<>();
        this.charStatus = new ArrayList<>();
        resetStats();
    }

    /** Reset all stats to zero (called when starting new game) */
    private void resetStats() {
        this.currentIndex = 0;
        this.currentErrors = 0;
        this.currentCorrectChars = 0;
        this.totalKeystrokes = 0;
        this.correctKeystrokes = 0;
        this.historyErrors = 0;
        this.historyCorrectChars = 0;
        this.historyTotalKeystrokes = 0;
        this.historyCorrectKeystrokes = 0;
        this.isRunning = false;
    }

    // ===== GAME LIFECYCLE =====

    /**
     * Start a new game with initial words.
     * Resets stats, loads words, records start time.
     */
    public void startGame(List<String> words) {
        resetStats();
        loadNewBatch(words);
        this.startTime = System.currentTimeMillis();
        this.isRunning = true;
    }

    /**
     * Reset start time to now. Used for "start timer on first keystroke" feature.
     */
    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Load new batch of words (for infinite scroll).
     * Saves current batch stats to history before loading new words.
     */
    public void loadNewBatch(List<String> newWords) {
        // Save current batch stats to history (for WPM calculation)
        this.historyCorrectChars += this.currentCorrectChars;
        this.historyErrors += this.currentErrors;

        // Save keystroke stats to history (for accuracy calculation)
        this.historyTotalKeystrokes += this.totalKeystrokes;
        this.historyCorrectKeystrokes += this.correctKeystrokes;

        // Reset for new batch
        this.currentCorrectChars = 0;
        this.currentErrors = 0;
        this.totalKeystrokes = 0;
        this.correctKeystrokes = 0;
        this.currentIndex = 0;

        // Load new words
        this.targetWords = newWords;
        this.charStatus.clear();

        // Prepare charStatus for each character (+1 for space after each word)
        int totalChars = 0;
        for (String w : newWords)
            totalChars += w.length() + 1;
        for (int i = 0; i < totalChars; i++)
            charStatus.add(null);
    }

    // ===== INPUT PROCESSING (CORE LOGIC) =====

    /**
     * Process each keystroke from user.
     * Compares input with target, updates stats, moves cursor.
     * 
     * KEYSTROKE TRACKING FOR ACCURACY:
     * - Every keystroke increments totalKeystrokes (even wrong ones)
     * - Only correct keystrokes increment correctKeystrokes
     * - This ensures errors lower accuracy even if corrected with backspace
     */
    public void processInput(char input, char target) {
        if (!isRunning)
            return;

        boolean isCorrect = (input == target);

        // Track keystroke for accuracy calculation
        // This is PERMANENT - backspace won't undo this
        totalKeystrokes++;
        if (isCorrect) {
            correctKeystrokes++;
        }

        // Save status for UI highlighting (green/red)
        if (currentIndex < charStatus.size()) {
            charStatus.set(currentIndex, isCorrect);
        }

        // Update display stats (these CAN be reversed by backspace)
        if (isCorrect) {
            currentCorrectChars++;
        } else {
            currentErrors++;
        }

        currentIndex++; // Move cursor to next character
    }

    /**
     * Process backspace - move cursor back one position.
     * Reverses the stats for the character being "un-typed".
     * 
     * @return true if backspace was successful, false if already at start
     */
    public boolean processBackspace() {
        if (!isRunning || currentIndex <= 0) {
            return false; // Can't go back further
        }

        // Move cursor back
        currentIndex--;

        // Get the status of the character we're "un-typing"
        if (currentIndex < charStatus.size()) {
            Boolean wasCorrect = charStatus.get(currentIndex);

            // Reverse the stats
            if (wasCorrect != null) {
                if (wasCorrect) {
                    currentCorrectChars--;
                } else {
                    currentErrors--;
                }
            }

            // Clear the status (mark as untyped)
            charStatus.set(currentIndex, null);
        }

        return true;
    }

    // ===== WPM CALCULATIONS =====

    /**
     * Calculate Gross WPM (raw speed including errors).
     * Formula: (Total Typed / 5) / Minutes
     * 5 chars = 1 word (industry standard)
     */
    public int calculateGrossWPM(long elapsedTimeMillis) {
        double minutes = elapsedTimeMillis / 60000.0;
        if (minutes < Constants.MIN_MINUTES_FOR_WPM)
            return 0;

        int totalTyped = historyCorrectChars + currentCorrectChars + historyErrors + currentErrors;
        return (int) ((totalTyped / (double) Constants.CHARACTERS_PER_WORD) / minutes);
    }

    /**
     * Calculate Net WPM (productive speed, only correct chars).
     * Formula: (Correct Chars / 5) / Minutes
     */
    public int calculateNetWPM(long elapsedTimeMillis) {
        double minutes = elapsedTimeMillis / 60000.0;
        if (minutes < Constants.MIN_MINUTES_FOR_WPM)
            return 0;

        int totalCorrect = historyCorrectChars + currentCorrectChars;
        return (int) ((totalCorrect / (double) Constants.CHARACTERS_PER_WORD) / minutes);
    }

    /** Alias for calculateNetWPM (backward compatibility) */
    public int calculateWPM(long elapsedTimeMillis) {
        return calculateNetWPM(elapsedTimeMillis);
    }

    // ===== ACCURACY CALCULATION =====

    /**
     * Calculate accuracy percentage using KEYSTROKE-BASED approach.
     * 
     * APPROACH (like Monkeytype/10FastFingers):
     * - Formula: (correctKeystrokes / totalKeystrokes) × 100
     * - Every keystroke counts, including errors
     * - Backspace allows visual correction, but error STILL lowers accuracy
     * - This prevents gaming the system by backspacing errors
     * 
     * EXAMPLE:
     * - Type "helo" (wrong) then backspace and type "hello" (correct)
     * - Total keystrokes = 9 (h-e-l-o-backspace-l-l-o... wait backspace doesn't
     * count)
     * - Total keystrokes = 4 (h-e-l-o) + 3 (l-l-o) = 7? No...
     * - Actually: "helo" = 4 keystrokes, then "llo" = 3 more = 7 total
     * - Correct = 3 (h-e-l from first) + 3 (l-l-o) = 6? Hmm...
     * - Simpler: Every key press that's NOT backspace adds to total
     * - Each correct key adds to correct count
     * 
     * @return Accuracy percentage (0-100)
     */
    public double calculateAccuracy() {
        // Sum up all keystroke stats (current batch + history)
        int allTotalKeystrokes = historyTotalKeystrokes + totalKeystrokes;
        int allCorrectKeystrokes = historyCorrectKeystrokes + correctKeystrokes;

        // Default 100% if no keystrokes yet
        if (allTotalKeystrokes == 0)
            return 100.0;

        return ((double) allCorrectKeystrokes / allTotalKeystrokes) * 100.0;
    }

    // ===== WEIGHTED SCORE =====

    /**
     * Calculate weighted score for ranking.
     * Formula: Net WPM × (Accuracy/100) ^ ACCURACY_WEIGHT
     * 
     * Higher ACCURACY_WEIGHT = more penalty for low accuracy.
     * Prevents "spammers" from getting high ranks.
     */
    public double calculateWeightedScore(long elapsedTimeMillis) {
        int netWPM = calculateNetWPM(elapsedTimeMillis);
        double accuracy = calculateAccuracy();
        double accuracyFactor = Math.pow(accuracy / 100.0, Constants.ACCURACY_WEIGHT);
        return netWPM * accuracyFactor;
    }

    // ===== STAT GETTERS =====

    public int getTotalErrors() {
        return historyErrors + currentErrors;
    }

    public int getTotalCorrectChars() {
        return historyCorrectChars + currentCorrectChars;
    }

    // ===== ABSTRACT METHOD (POLYMORPHISM) =====

    /**
     * Abstract method - MUST be overridden by child classes.
     * Determines what happens when game ends.
     * 
     * POLYMORPHISM:
     * - TimeGame: Saves score to database
     * - CustomGame: Just prints to console (sandbox mode)
     */
    public abstract void endGame(User user, int timeSetting, String language);

    // ===== STATE GETTERS/SETTERS =====

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<Boolean> getCharStatus() {
        return charStatus;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /** Stop game manually (e.g., when user clicks "Give Up") */
    public void stopGame() {
        this.isRunning = false;
    }

    /** Protected setter for child classes */
    protected void setRunning(boolean running) {
        this.isRunning = running;
    }
}