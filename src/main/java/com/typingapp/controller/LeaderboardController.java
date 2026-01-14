package com.typingapp.controller;

import com.typingapp.database.DatabaseHelper;
import com.typingapp.model.Score;
import com.typingapp.model.User;
import com.typingapp.util.AlertHelper;
import com.typingapp.util.NavigationHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * LeaderboardController - Leaderboard screen controller
 * 
 * Displays player rankings based on weighted score.
 * 
 * Responsibilities:
 * - Display leaderboard with filters
 * - Filter by time (15s / 30s / 60s)
 * - Filter by language (All / Indonesia / English)
 * - Search by username
 * - Auto-refresh when filters change
 * 
 * Ranking: Sorted by weighted_score (WPM × Accuracy factor)
 */
public class LeaderboardController {

    // ===== UI COMPONENTS - FILTERS =====
    @FXML
    private TextField searchField; // Username search
    @FXML
    private ComboBox<String> timeFilterCombo; // Time filter
    @FXML
    private ComboBox<String> languageFilterCombo; // Language filter

    // ===== UI COMPONENTS - TABLE =====
    @FXML
    private TableView<Score> leaderboardTable;
    @FXML
    private TableColumn<Score, String> rankColumn; // Rank (#)
    @FXML
    private TableColumn<Score, String> usernameColumn; // Username
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
     * Sets up table columns, filters, and event listeners.
     */
    public void initialize() {
        // Table column bindings
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        languageColumn.setCellValueFactory(new PropertyValueFactory<>("language"));
        wpmColumn.setCellValueFactory(new PropertyValueFactory<>("wpm"));
        accuracyColumn.setCellValueFactory(new PropertyValueFactory<>("accuracyFormatted"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));

        // Rank column: calculated from table row index
        rankColumn.setCellFactory(column -> new TableCell<Score, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        // Time filter setup
        timeFilterCombo.getItems().addAll("15s", "30s", "60s");
        timeFilterCombo.setValue("30s");

        // Language filter setup
        languageFilterCombo.getItems().addAll("All", "English", "Indonesia");
        languageFilterCombo.setValue("All");

        // Load initial data
        loadLeaderboard();

        // Auto-refresh on filter changes
        timeFilterCombo.setOnAction(e -> loadLeaderboard());
        languageFilterCombo.setOnAction(e -> loadLeaderboard());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadLeaderboard());
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

    /**
     * Load leaderboard from database with active filters.
     * 
     * Filters applied:
     * 1. Time Mode (15s / 30s / 60s)
     * 2. Language (All / Indonesia / English)
     * 3. Username search (partial match)
     */
    private void loadLeaderboard() {
        String timeFilter = timeFilterCombo.getValue();
        int timeMode = Integer.parseInt(timeFilter.replace("s", "")); // "30s" -> 30
        String languageFilter = languageFilterCombo.getValue();
        String searchQuery = searchField.getText().trim();

        List<Score> scores = DatabaseHelper.getLeaderboard(timeMode, languageFilter, searchQuery);
        leaderboardTable.setItems(FXCollections.observableArrayList(scores));
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
}