package com.typingapp;

import com.typingapp.controller.LoginController;
import com.typingapp.database.DatabaseHelper;
import com.typingapp.util.Constants;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * MainApp - Application Entry Point
 * 
 * This is the main class that starts the DurianType typing game.
 * It extends JavaFX Application to create the GUI window.
 * 
 * Program Flow:
 * 1. JVM calls main() → launch() starts JavaFX
 * 2. JavaFX calls start() → window is created and shown
 */
public class MainApp extends Application {

    /**
     * Entry point called by JVM.
     * Simply delegates to launch() which triggers the JavaFX lifecycle.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes and displays the main application window.
     * Called automatically by JavaFX after launch().
     * 
     * Steps:
     * 1. Initialize SQLite database (create tables if needed)
     * 2. Load login screen from FXML
     * 3. Connect controller to stage for navigation
     * 4. Apply CSS theme styling
     * 5. Configure window size and show
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database - creates tables if they don't exist
            DatabaseHelper.initDB();

            // Load login screen from FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.FXML_LOGIN));
            Parent root = loader.load();

            // Give controller a reference to stage (needed for navigation)
            LoginController controller = loader.getController();
            controller.setStage(primaryStage);

            // Create scene and apply CSS stylesheet
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(Constants.CSS_STYLE).toExternalForm());

            // Configure and show window
            primaryStage.setTitle("DurianType - Typing Game");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setMaximized(true); // Start maximized
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace(); // Print error for debugging
        }
    }
}