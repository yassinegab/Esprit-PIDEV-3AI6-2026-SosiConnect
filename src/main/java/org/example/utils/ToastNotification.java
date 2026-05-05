package org.example.utils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Modern toast notification system for JavaFX.
 * Slides in from the top-right with auto-dismiss.
 */
public class ToastNotification {

    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * Show a toast notification on the given owner StackPane.
     * If owner is null, attempts to find a suitable parent.
     */
    public static void show(StackPane owner, String message, ToastType type, double durationSeconds) {
        if (owner == null) return;

        HBox toast = buildToast(message, type);

        // Position at top-right
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        toast.setTranslateX(400); // start off-screen right
        toast.setTranslateY(16);
        toast.setOpacity(0);

        owner.getChildren().add(toast);

        // Slide in
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), toast);
        slideIn.setFromX(400);
        slideIn.setToX(-16);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition showAnim = new ParallelTransition(slideIn, fadeIn);

        // Auto-dismiss after duration
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));

        // Slide out
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toast);
        slideOut.setToX(400);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
        fadeOut.setToValue(0);

        ParallelTransition hideAnim = new ParallelTransition(slideOut, fadeOut);
        hideAnim.setOnFinished(e -> owner.getChildren().remove(toast));

        SequentialTransition sequence = new SequentialTransition(showAnim, pause, hideAnim);
        sequence.play();

        // Click to dismiss early
        toast.setOnMouseClicked(e -> {
            sequence.stop();
            hideAnim.setOnFinished(ev -> owner.getChildren().remove(toast));
            hideAnim.play();
        });
    }

    /** Convenience: show with default 3-second duration */
    public static void show(StackPane owner, String message, ToastType type) {
        show(owner, message, type, 3.0);
    }

    private static HBox buildToast(String message, ToastType type) {
        String emoji;
        String bgColor;
        String borderColor;
        String textColor;

        switch (type) {
            case SUCCESS:
                emoji = "\u2705";
                bgColor = "linear-gradient(to right, #f0fdf4, #dcfce7)";
                borderColor = "#86efac";
                textColor = "#166534";
                break;
            case ERROR:
                emoji = "\u274C";
                bgColor = "linear-gradient(to right, #fef2f2, #fee2e2)";
                borderColor = "#fca5a5";
                textColor = "#991b1b";
                break;
            case WARNING:
                emoji = "\u26A0\uFE0F";
                bgColor = "linear-gradient(to right, #fffbeb, #fef3c7)";
                borderColor = "#fcd34d";
                textColor = "#92400e";
                break;
            case INFO:
            default:
                emoji = "\u2139\uFE0F";
                bgColor = "linear-gradient(to right, #eff6ff, #dbeafe)";
                borderColor = "#93c5fd";
                textColor = "#1e40af";
                break;
        }

        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 18px;");

        Label text = new Label(message);
        text.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + textColor + "; -fx-font-family: 'Segoe UI';");
        text.setWrapText(true);
        text.setMaxWidth(450);

        Label closeHint = new Label("\u2715");
        closeHint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textColor + "; -fx-opacity: 0.5; -fx-cursor: hand;");

        HBox toast = new HBox(10, icon, text, closeHint);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(540);
        toast.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 14;" +
                "-fx-padding: 12 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 6);" +
                "-fx-cursor: hand;"
        );

        toast.setPickOnBounds(false);
        toast.setMouseTransparent(false);

        return toast;
    }

    /**
     * Utility to find a StackPane owner from any Node in the scene graph.
     */
    public static StackPane findOwner(Node node) {
        if (node == null) return null;
        Node current = node;
        while (current != null) {
            if (current instanceof StackPane) return (StackPane) current;
            current = current.getParent();
        }
        // Fallback: wrap scene root if possible
        if (node.getScene() != null && node.getScene().getRoot() instanceof StackPane) {
            return (StackPane) node.getScene().getRoot();
        }
        return null;
    }
}
