package com.typingapp.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

/**
 * AlertHelper - Utility for showing dialog boxes
 * 
 * Eliminates duplicate alert creation code across controllers.
 * All methods are STATIC - call directly: AlertHelper.showInfo(...)
 * 
 * Alert Types:
 * - INFORMATION: Success notifications
 * - ERROR: Error messages
 * - WARNING: Non-critical warnings
 * - CONFIRMATION: OK/Cancel dialogs
 */
public final class AlertHelper {

    // Private constructor - utility class pattern
    private AlertHelper() {
        throw new UnsupportedOperationException("AlertHelper class cannot be instantiated");
    }

    /**
     * Shows info alert for success messages.
     * Example: "Account created successfully!"
     */
    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait(); // Blocks until user clicks OK
    }

    /**
     * Shows error alert for system errors.
     * Example: "Failed to load menu"
     */
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows warning alert for non-critical issues.
     * Example: "Low accuracy detected"
     */
    public static void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Shows confirmation dialog with OK and Cancel buttons.
     * Returns true if user clicks OK, false otherwise.
     * Example: "Delete account?" -> OK to delete, Cancel to abort
     */
    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Shows basic game over dialog.
     * Used for backward compatibility.
     */
    public static void showGameOver(boolean isCustomMode, int wpm, double accuracy) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(isCustomMode ? "Custom Game Finished!" : "Time's Up!");
        alert.setContentText(
                "WPM: " + wpm + "\n" +
                        "Accuracy: " + String.format("%.1f%%", accuracy));
        alert.showAndWait();
    }

    /**
     * Shows enhanced game over dialog with detailed stats.
     * Displays: Net WPM, Gross WPM, Accuracy, Weighted Score
     * Also shows error penalty if applicable.
     */
    public static void showGameOverEnhanced(boolean isCustomMode, int netWpm, int grossWpm,
            double accuracy, double weightedScore) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(isCustomMode ? "Custom Game Finished!" : "Time's Up!");

        StringBuilder content = new StringBuilder();
        content.append("📊 Your Results:\n\n");
        content.append(String.format("Net WPM: %d\n", netWpm));
        content.append(String.format("Raw WPM: %d\n", grossWpm));
        content.append(String.format("Accuracy: %.1f%%\n", accuracy));
        content.append(String.format("Score: %.1f\n", weightedScore));

        // Show error penalty if there's a difference
        if (grossWpm > netWpm) {
            int errorPenalty = grossWpm - netWpm;
            content.append(String.format("\n⚠️ Error Penalty: -%d WPM", errorPenalty));
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }
}
