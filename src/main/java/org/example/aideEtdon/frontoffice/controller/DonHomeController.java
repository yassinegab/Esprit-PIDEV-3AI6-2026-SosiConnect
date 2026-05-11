package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class DonHomeController {

    @FXML
    private VBox cardCreate;

    @FXML
    private VBox cardView;

    @FXML
    private VBox cardVideo;

    @FXML
    public void initialize() {
        setupCardHoverAnimation(cardCreate);
        setupCardHoverAnimation(cardView);
        setupCardHoverAnimation(cardVideo);

        // Staggered entrance animation for premium feel
        animateCardEntrance(cardCreate, 0);
        animateCardEntrance(cardView, 100);
        animateCardEntrance(cardVideo, 200);
    }

    private void animateCardEntrance(VBox card, double delayMs) {
        card.setOpacity(0);
        card.setTranslateY(40);
        card.setScaleX(0.92);
        card.setScaleY(0.92);

        FadeTransition ft = new FadeTransition(Duration.millis(600), card);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));

        TranslateTransition tt = new TranslateTransition(Duration.millis(600), card);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));

        ScaleTransition st = new ScaleTransition(Duration.millis(600), card);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setDelay(Duration.millis(delayMs));

        ft.play();
        tt.play();
        st.play();
    }

    private void setupCardHoverAnimation(VBox card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(250), card);
            scaleTransition.setToX(1.03);
            scaleTransition.setToY(1.03);
            scaleTransition.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(250), card);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();
        });

        // Tactile press feedback
        card.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), card);
            st.setToX(0.98);
            st.setToY(0.98);
            st.play();
        });
        card.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
    }

    @FXML
    public void goToCreateDemande() {
        navigateTo("/aideEtdon/frontoffice/DemandeFormView.fxml");
    }

    @FXML
    public void goToViewDemandes() {
        navigateTo("/aideEtdon/frontoffice/DemandeListView.fxml");
    }

    @FXML
    public void goToVideos() {
        navigateTo("/aideEtdon/frontoffice/VideoListView.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            AideEtdonControllerClientController.getInstance().setView(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
