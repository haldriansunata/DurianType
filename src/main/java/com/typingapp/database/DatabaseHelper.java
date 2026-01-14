package com.typingapp.database;

import com.typingapp.model.Score;
import com.typingapp.model.User;
import com.typingapp.util.Constants;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseHelper - SQLite database operations
 * 
 * Handles all database interactions for users and scores.
 * 
 * Responsibilities:
 * - Initialize database and create tables
 * - User CRUD (Create, Read, Update, Delete)
 * - Score CRUD operations
 * - Leaderboard and history queries
 * 
 * Scoring System:
 * - Net WPM: Speed after error penalty
 * - Gross WPM: Raw speed without penalty
 * - Weighted Score: WPM × (Accuracy/100)^1.5
 */
public class DatabaseHelper {

    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());

    /**
     * Initialize database - creates tables if they don't exist.
     * Called once when app starts.
     * 
     * Tables created:
     * - users: id, username (unique), password
     * - scores: id, user_id (FK), wpm, gross_wpm, accuracy, weighted_score,
     * time_mode, language, date
     */
    public static void initDB() {
        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                Statement stmt = conn.createStatement()) {

            // Create users table
            String sqlUser = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password TEXT NOT NULL" +
                    ")";
            stmt.execute(sqlUser);

            // Create scores table
            String sqlScore = "CREATE TABLE IF NOT EXISTS scores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "wpm INTEGER, " +
                    "gross_wpm INTEGER, " +
                    "accuracy REAL, " +
                    "weighted_score REAL, " +
                    "time_mode INTEGER, " +
                    "language TEXT, " +
                    "date_played TEXT, " +
                    "FOREIGN KEY(user_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(sqlScore);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    // ===== USER MANAGEMENT =====

    /**
     * Register new user.
     * 
     * @return true if success, false if username already exists
     */
    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Registration failed: " + username, e);
            return false; // Username already exists (UNIQUE constraint)
        }
    }

    /**
     * Verify credentials and return User object.
     * 
     * @return User if valid, null if invalid
     */
    public static User loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login error: " + username, e);
        }
        return null;
    }

    /**
     * Update user profile.
     * If password is PASSWORD_PLACEHOLDER, only update username.
     */
    public static boolean updateUser(int userId, String newName, String newPass) {
        String sql = newPass.equals(Constants.PASSWORD_PLACEHOLDER)
                ? "UPDATE users SET username = ? WHERE id = ?"
                : "UPDATE users SET username = ?, password = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (newPass.equals(Constants.PASSWORD_PLACEHOLDER)) {
                pstmt.setString(1, newName);
                pstmt.setInt(2, userId);
            } else {
                pstmt.setString(1, newName);
                pstmt.setString(2, newPass);
                pstmt.setInt(3, userId);
            }

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to update user: " + userId, e);
            return false;
        }
    }

    /**
     * Delete user and all their scores.
     * Deletes scores first (foreign key), then user.
     */
    public static void deleteUser(int userId) {
        try (Connection conn = DriverManager.getConnection(Constants.DB_URL)) {
            // Delete scores first
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM scores WHERE user_id = ?")) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
            // Then delete user
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete user: " + userId, e);
        }
    }

    // ===== SCORE MANAGEMENT =====

    /**
     * Save game score to database with all metrics.
     */
    public static void addScore(int userId, int netWpm, int grossWpm, double accuracy,
            double weightedScore, int timeMode, String language) {
        String sql = "INSERT INTO scores(user_id, wpm, gross_wpm, accuracy, weighted_score, time_mode, language, date_played) "
                +
                "VALUES(?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'))";
        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, netWpm);
            pstmt.setInt(3, grossWpm);
            pstmt.setDouble(4, accuracy);
            pstmt.setDouble(5, weightedScore);
            pstmt.setInt(6, timeMode);
            pstmt.setString(7, language);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add score for user: " + userId, e);
        }
    }

    /** Legacy method - auto-calculates weightedScore */
    public static void addScore(int userId, int wpm, double accuracy, int timeMode) {
        double weightedScore = wpm * Math.pow(accuracy / 100.0, Constants.ACCURACY_WEIGHT);
        addScore(userId, wpm, wpm, accuracy, weightedScore, timeMode, Constants.LANG_INDONESIA);
    }

    // ===== LEADERBOARD =====

    /**
     * Get leaderboard with filters.
     * Sorted by weighted_score (highest first).
     * 
     * @param timeFilter     Game duration filter (15/30/60)
     * @param languageFilter "All" or specific language
     * @param nameSearch     Partial username match
     */
    public static List<Score> getLeaderboard(int timeFilter, String languageFilter, String nameSearch) {
        List<Score> scores = new ArrayList<>();

        // Add s.language to SELECT
        StringBuilder sql = new StringBuilder(
                "SELECT u.username, s.wpm, s.gross_wpm, s.accuracy, s.weighted_score, s.date_played, s.language " +
                        "FROM scores s JOIN users u ON s.user_id = u.id " +
                        "WHERE s.time_mode = ? AND u.username LIKE ? AND s.accuracy >= ? ");

        if (languageFilter != null && !languageFilter.equalsIgnoreCase("All")) {
            sql.append("AND s.language = ? ");
        }
        sql.append("ORDER BY weighted_score DESC LIMIT ?");

        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            pstmt.setInt(i++, timeFilter);
            pstmt.setString(i++, "%" + nameSearch + "%");
            pstmt.setDouble(i++, Constants.MIN_ACCURACY_FOR_LEADERBOARD);
            if (languageFilter != null && !languageFilter.equalsIgnoreCase("All")) {
                pstmt.setString(i++, languageFilter);
            }
            pstmt.setInt(i++, Constants.LEADERBOARD_LIMIT);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Use new constructor with language
                scores.add(new Score(
                        rs.getString("username"),
                        rs.getInt("wpm"),
                        rs.getInt("gross_wpm"),
                        rs.getDouble("accuracy"),
                        rs.getDouble("weighted_score"),
                        rs.getString("date_played"),
                        rs.getString("language")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load leaderboard", e);
        }
        return scores;
    }

    /** Overload without language filter */
    public static List<Score> getLeaderboard(int timeFilter, String nameSearch) {
        return getLeaderboard(timeFilter, "All", nameSearch);
    }

    // ===== PERSONAL HISTORY =====

    /**
     * Get user's game history with optional language filter.
     * Sorted by newest first (ID DESC).
     */
    public static List<Score> getHistory(int userId, String languageFilter) {
        List<Score> scores = new ArrayList<>();

        // Add s.language to SELECT
        StringBuilder sql = new StringBuilder(
                "SELECT u.username, s.wpm, s.gross_wpm, s.accuracy, s.weighted_score, s.time_mode, s.date_played, s.language "
                        +
                        "FROM scores s JOIN users u ON s.user_id = u.id " +
                        "WHERE s.user_id = ? ");

        if (languageFilter != null && !languageFilter.equalsIgnoreCase("All")) {
            sql.append("AND s.language = ? ");
        }
        sql.append("ORDER BY s.id DESC");

        try (Connection conn = DriverManager.getConnection(Constants.DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setInt(1, userId);
            if (languageFilter != null && !languageFilter.equalsIgnoreCase("All")) {
                pstmt.setString(2, languageFilter);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Use new constructor with language
                scores.add(new Score(
                        rs.getString("username"),
                        rs.getInt("wpm"),
                        rs.getInt("gross_wpm"),
                        rs.getDouble("accuracy"),
                        rs.getDouble("weighted_score"),
                        rs.getString("date_played"),
                        rs.getInt("time_mode"),
                        rs.getString("language")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load history for user: " + userId, e);
        }
        return scores;
    }

    /** Overload without language filter */
    public static List<Score> getHistory(int userId) {
        return getHistory(userId, "All");
    }
}