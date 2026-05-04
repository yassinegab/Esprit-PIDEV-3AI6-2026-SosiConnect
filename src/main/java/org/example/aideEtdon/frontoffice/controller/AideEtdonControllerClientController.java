package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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
    }

    public static AideEtdonControllerClientController getInstance() {
        return instance;
    }

    public void setView(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
        applyAnimation(node);
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

    private void applyAnimation(Node pane) {
        pane.setOpacity(0);

        FadeTransition fade = new FadeTransition(Duration.millis(400), pane);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(400), pane);
        translate.setFromY(20);
        translate.setToY(0);

        fade.play();
        translate.play();
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
