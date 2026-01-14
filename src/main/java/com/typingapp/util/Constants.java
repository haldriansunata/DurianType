package com.typingapp.util;

/**
 * Constants - Application-wide constant values
 * 
 * Stores all "magic strings" and "magic numbers" in one place.
 * Benefits:
 * - Change values in one place, applies everywhere
 * - Code is more readable (BATCH_SIZE vs 25)
 * - Reduces typos and inconsistencies
 * 
 * All constants are: public (accessible anywhere), static (no instance needed),
 * final (immutable)
 */
public final class Constants {

    // Private constructor prevents instantiation (utility class pattern)
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ===== DATABASE =====
    public static final String DB_URL = "jdbc:sqlite:typing_game.db"; // SQLite database file

    // ===== FXML PATHS =====
    // Paths to screen layout files (located in /fxml/ folder)
    public static final String FXML_LOGIN = "/fxml/login.fxml";
    public static final String FXML_MENU = "/fxml/menu.fxml";
    public static final String FXML_GAME = "/fxml/game.fxml";
    public static final String FXML_CUSTOM = "/fxml/custom.fxml";
    public static final String FXML_LEADERBOARD = "/fxml/leaderboard.fxml";
    public static final String FXML_PROFILE = "/fxml/profile.fxml";

    // ===== DATA FILES =====
    public static final String DATA_WORDS_ID = "/data/words_id.txt"; // Indonesian word list
    public static final String DATA_WORDS_EN = "/data/words_en.txt"; // English word list

    // ===== CSS =====
    public static final String CSS_STYLE = "/css/style.css"; // Main stylesheet

    // ===== GAME CONFIG =====
    public static final int BATCH_SIZE = 25; // Words displayed per batch
    public static final int MAX_WORDS_POOL = 300; // Max words loaded in memory
    public static final int MIN_WORDS_FOR_CUSTOM = 10; // Min words for custom mode
    public static final double MIN_MINUTES_FOR_WPM = 0.0167; // ~1 second, prevents div by zero
    public static final int CHARACTERS_PER_WORD = 5; // Industry standard: 5 chars = 1 word

    // ===== SCORING =====
    // Weighted Score = Net WPM × (Accuracy/100)^ACCURACY_WEIGHT
    // Higher weight = more penalty for low accuracy
    public static final double ACCURACY_WEIGHT = 1.5;
    public static final double MIN_ACCURACY_FOR_LEADERBOARD = 0.0; // 0 means show all scores

    // ===== TIME MODES =====
    public static final int TIME_MODE_15 = 15; // 15 seconds
    public static final int TIME_MODE_30 = 30; // 30 seconds
    public static final int TIME_MODE_60 = 60; // 60 seconds

    // ===== LANGUAGES =====
    public static final String LANG_INDONESIA = "Indonesia";
    public static final String LANG_ENGLISH = "English";
    public static final String LANG_CUSTOM = "Custom";

    // ===== PASSWORD =====
    public static final String PASSWORD_PLACEHOLDER = "placeholder"; // Indicates "don't change"
    public static final int MIN_PASSWORD_LENGTH = 4;

    // ===== UI MESSAGES =====
    public static final String MSG_GUEST = "Guest";
    public static final String MSG_HELLO_PREFIX = "Hello, ";

    // ===== LEADERBOARD =====
    public static final int LEADERBOARD_LIMIT = 50; // Max entries shown
}
