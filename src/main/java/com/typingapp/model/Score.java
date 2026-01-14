package com.typingapp.model;

/**
 * Score - Represents a game result
 * 
 * Uses ENCAPSULATION (OOP concept):
 * - All attributes are private
 * - Access only through getter methods
 * 
 * Stores performance metrics:
 * - WPM (Net and Gross)
 * - Accuracy percentage
 * - Weighted Score (for ranking)
 * 
 * Used for: Leaderboard display, Personal history, Data transfer between DB and
 * UI
 */
public class Score {

    private String username; // Player who achieved this score
    private int wpm; // Net WPM (speed after error penalty)
    private int grossWpm; // Gross WPM (raw speed without penalty)
    private double accuracy; // Accuracy percentage (0-100)
    private double weightedScore; // Combined score: WPM × Accuracy factor
    private String date; // Date played (from database)
    private int timeMode; // Game duration: 15, 30, or 60 seconds
    private String language; // Language played (Indonesia/English)

    /**
     * Full constructor for leaderboard data.
     * Contains all metrics from database.
     */
    public Score(String username, int wpm, int grossWpm, double accuracy, double weightedScore, String date,
            String language) {
        this.username = username;
        this.wpm = wpm;
        this.grossWpm = grossWpm;
        this.accuracy = accuracy;
        this.weightedScore = weightedScore;
        this.date = date;
        this.language = language;
    }

    /**
     * Legacy constructor for backward compatibility.
     * Auto-calculates grossWpm and weightedScore.
     */
    public Score(String username, int wpm, double accuracy, String date) {
        this.username = username;
        this.wpm = wpm;
        this.grossWpm = wpm; // Assume gross = net for legacy data
        this.accuracy = accuracy;
        this.weightedScore = wpm * Math.pow(accuracy / 100.0, 1.5);
        this.date = date;
    }

    /**
     * Constructor for history with time mode info.
     */
    public Score(String username, int wpm, double accuracy, String date, int timeMode) {
        this.username = username;
        this.wpm = wpm;
        this.grossWpm = wpm;
        this.accuracy = accuracy;
        this.weightedScore = wpm * Math.pow(accuracy / 100.0, 1.5);
        this.date = date;
        this.timeMode = timeMode;
    }

    /**
     * Full constructor with all metrics and time mode.
     * Used for complete history queries.
     */
    public Score(String username, int wpm, int grossWpm, double accuracy, double weightedScore, String date,
            int timeMode, String language) {
        this.username = username;
        this.wpm = wpm;
        this.grossWpm = grossWpm;
        this.accuracy = accuracy;
        this.weightedScore = weightedScore;
        this.date = date;
        this.timeMode = timeMode;
        this.language = language;
    }

    // ===== GETTERS =====
    public String getUsername() {
        return username;
    }

    public int getWpm() {
        return wpm;
    }

    public int getGrossWpm() {
        return grossWpm;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getWeightedScore() {
        return weightedScore;
    }

    public String getDate() {
        return date;
    }

    public int getTimeMode() {
        return timeMode;
    }

    public String getLanguage() {
        return language;
    }

    // ===== FORMATTED GETTERS (for UI display) =====

    /** Returns accuracy as "95.5%" */
    public String getAccuracyFormatted() {
        return String.format("%.1f%%", accuracy);
    }

    /** Returns time mode as "30s" */
    public String getTimeModeFormatted() {
        return timeMode + "s";
    }

    /** Returns weighted score as "85.3" */
    public String getWeightedScoreFormatted() {
        return String.format("%.1f", weightedScore);
    }

    /** Returns "Net: 80 | Gross: 95" */
    public String getWpmDetailed() {
        return String.format("Net: %d | Gross: %d", wpm, grossWpm);
    }

    /**
     * Returns date in dd/MM/yyyy | HH:mm:ss format.
     * Converts from database format (yyyy-MM-dd HH:mm:ss).
     */
    public String getDateFormatted() {
        if (date == null || date.isEmpty())
            return "";
        try {
            // date comes as "2024-12-25 15:30:00"
            String[] parts = date.split(" ");
            if (parts.length >= 2) {
                String datePart = parts[0]; // 2024-12-25
                String timePart = parts[1]; // 15:30:00
                String[] dateParts = datePart.split("-");
                if (dateParts.length == 3) {
                    // Convert to dd/MM/yyyy | HH:mm:ss
                    // dateParts[0]=yyyy, dateParts[1]=MM, dateParts[2]=dd
                    return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0] + " | " + timePart;
                }
            }
            return date;
        } catch (Exception e) {
            return date;
        }
    }
}
