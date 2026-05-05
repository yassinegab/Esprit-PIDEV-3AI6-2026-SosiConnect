package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import org.example.utils.ToastNotification;

import java.io.IOException;

public class AideEtdonControllerClientController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnAides;

    @FXML
    private Button btnDons;

    @FXML
    private Button btnMesDemandes;

    private boolean isAideActive = false;
    private boolean isDonActive = false;

    private static AideEtdonControllerClientController instance;

    private void setNavActive(Button active) {
        btnAides.getStyleClass().removeAll("nav-pill-active");
        btnDons.getStyleClass().removeAll("nav-pill-active");
        btnMesDemandes.getStyleClass().removeAll("nav-pill-active");
        active.getStyleClass().add("nav-pill-active");
    }

    @FXML
    public void initialize() {
        instance = this;
        btnMesDemandes.setVisible(false);
        btnMesDemandes.setManaged(false);
        showAides();

        setupButtonHoverAnimation(btnAides);
        setupButtonHoverAnimation(btnDons);
        setupButtonHoverAnimation(btnMesDemandes);

        btnAides.setTooltip(new Tooltip("Accéder au service d'aide d'urgence"));
        btnDons.setTooltip(new Tooltip("Parcourir et répondre aux demandes de dons"));
        btnMesDemandes.setTooltip(new Tooltip("Voir mes demandes et statistiques"));
    }

    public static AideEtdonControllerClientController getInstance() {
        return instance;
    }

    public StackPane getContentArea() {
        return contentArea;
    }

    /**
     * Shows a toast notification after a delay, so it survives view transitions.
     * Call this AFTER handleRetour/navigation — the toast appears once the new view is loaded.
     */
    public void showToast(String message, ToastNotification.ToastType type, double durationSeconds) {
        PauseTransition delay = new PauseTransition(Duration.millis(600));
        delay.setOnFinished(e -> ToastNotification.show(contentArea, message, type, durationSeconds));
        delay.play();
    }

    public void setView(Node node) {
        // Crossfade: fade out old content, then swap and fade in new
        if (!contentArea.getChildren().isEmpty()) {
            Node oldNode = contentArea.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), oldNode);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(node);
                applyEntranceAnimation(node);
            });
            fadeOut.play();
        } else {
            contentArea.getChildren().add(node);
            applyEntranceAnimation(node);
        }
    }

    private void setupButtonHoverAnimation(Button btn) {
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.05);
            btn.setScaleY(1.05);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
    }

    private void applyEntranceAnimation(Node pane) {
        pane.setOpacity(0);
        pane.setTranslateY(18);
        pane.setScaleX(0.97);
        pane.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(350), pane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(350), pane);
        slide.setFromY(18);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(350), pane);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide, scale).play();
    }

    @FXML
    public void showAides() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/AideHomeView.fxml"));
            Node aideView = loader.load();
            setView(aideView);
            
            setNavActive(btnAides);
            btnMesDemandes.setVisible(false);
            btnMesDemandes.setManaged(false);
            
            isAideActive = true;
            isDonActive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showDons() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/DonHomeView.fxml"));
            Node donView = loader.load();
            setView(donView);
            
            setNavActive(btnDons);
            btnMesDemandes.setVisible(true);
            btnMesDemandes.setManaged(true);
            isDonActive = true;
            isAideActive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showMesDemandes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/MesDemandesView.fxml"));
            Node mesDemandesView = loader.load();
            setView(mesDemandesView);

            setNavActive(btnMesDemandes);
            isAideActive = false;
            isDonActive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
