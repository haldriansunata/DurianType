package com.typingapp.util;

import com.typingapp.controller.*;
import com.typingapp.engine.GameEngine;
import com.typingapp.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * NavigationHelper - Utility for screen navigation
 * 
 * Centralizes all FXML loading and scene switching logic.
 * Benefits:
 * - Navigation code in one place
 * - Reduces duplication in controllers
 * - Easy to maintain and debug
 * 
 * All methods are STATIC for easy calling.
 */
public class NavigationHelper {

    /**
     * Navigate to login screen.
     * Used when logging out or after account deletion.
     */
    public static void goToLogin(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_LOGIN));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setStage(stage);

        setScene(stage, root);
    }

    /**
     * Navigate to main menu with default settings.
     * Used after initial login.
     */
    public static void goToMenu(User user, Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_MENU));
        Parent root = loader.load();

        MenuController controller = loader.getController();
        controller.setUserAndStage(user, stage);

        setScene(stage, root);
    }

    /**
     * Navigate to menu and restore previous language/time settings.
     * Used when returning from other screens (Game, Profile, etc).
     */
    public static void goToMenu(User user, Stage stage, String language, int time) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_MENU));
        Parent root = loader.load();

        MenuController controller = loader.getController();
        controller.setUserAndStage(user, stage);
        controller.restoreSettings(language, time);

        setScene(stage, root);
    }

    /** Alias for goToMenu with settings (for GameController compatibility) */
    public static void goToMenuWithSettings(User user, Stage stage, String language, int time) throws IOException {
        goToMenu(user, stage, language, time);
    }

    /**
     * Navigate to leaderboard screen.
     * Saves previous settings for Back button.
     */
    public static void goToLeaderboard(User user, Stage stage, String prevLang, int prevTime) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_LEADERBOARD));
        Parent root = loader.load();

        LeaderboardController controller = loader.getController();
        controller.setUserAndStage(user, stage);
        controller.setReturnState(prevLang, prevTime);

        setScene(stage, root);
    }

    /** Overload without saved state */
    public static void goToLeaderboard(User user, Stage stage) throws IOException {
        goToLeaderboard(user, stage, null, 0);
    }

    /**
     * Navigate to profile screen.
     * Saves previous settings for Back button.
     */
    public static void goToProfile(User user, Stage stage, String prevLang, int prevTime) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_PROFILE));
        Parent root = loader.load();

        ProfileController controller = loader.getController();
        controller.setUserAndStage(user, stage);
        controller.setReturnState(prevLang, prevTime);

        setScene(stage, root);
    }

    /** Overload without saved state */
    public static void goToProfile(User user, Stage stage) throws IOException {
        goToProfile(user, stage, null, 0);
    }

    /**
     * Navigate to custom text input screen.
     */
    public static void goToCustom(User user, Stage stage, String prevLang, int prevTime) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_CUSTOM));
        Parent root = loader.load();

        CustomController controller = loader.getController();
        controller.setUserAndStage(user, stage);
        controller.setReturnState(prevLang, prevTime);

        setScene(stage, root);
    }

    /** Overload without saved state */
    public static void goToCustom(User user, Stage stage) throws IOException {
        goToCustom(user, stage, null, 0);
    }

    /**
     * Navigate to game screen with all required parameters.
     * Most complex navigation - sends lots of data to GameController.
     * 
     * @param engine      Game engine (TimeGame or CustomGame)
     * @param words       List of words to type
     * @param isCustom    True for custom mode, false for time mode
     * @param timeSetting Duration in seconds (0 for infinite/custom)
     * @param user        Current user (null for guest)
     * @param stage       Application window
     * @param language    Selected language
     */
    public static void goToGame(GameEngine engine, List<String> words, boolean isCustom, int timeSetting,
            User user, Stage stage, String language) throws IOException {
        // For non-custom mode, restoreTime = timeSetting
        goToGame(engine, words, isCustom, timeSetting, user, stage, language, timeSetting);
    }

    /**
     * Navigate to game screen with explicit restoreTime.
     * Used by Custom mode to preserve original menu time settings.
     * 
     * @param restoreTime Time to restore when returning to menu (for Custom mode)
     */
    public static void goToGame(GameEngine engine, List<String> words, boolean isCustom, int timeSetting,
            User user, Stage stage, String language, int restoreTime) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationHelper.class.getResource(Constants.FXML_GAME));
        Parent root = loader.load();

        GameController controller = loader.getController();
        controller.setGameData(engine, words, isCustom, timeSetting, user, stage, language, restoreTime);

        setScene(stage, root);
    }

    /**
     * Helper method to switch scenes.
     * - If scene doesn't exist: create new scene and apply CSS
     * - If scene exists: just change the root (more efficient, CSS stays loaded)
     */
    private static void setScene(Stage stage, Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root);
            scene.getStylesheets().add(NavigationHelper.class.getResource(Constants.CSS_STYLE).toExternalForm());
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root); // Reuse scene, just change content
        }
    }
}
