package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.aideEtdon.model.Demande;
import org.example.aideEtdon.service.DemandeService;

import java.io.IOException;
import java.util.List;

public class DemandeListController {

    @FXML private FlowPane cardsContainer;
    private DemandeService demandeService;

    @FXML
    public void initialize() {
        demandeService = new DemandeService();
        showSkeletonLoading();

        // Simulate data load with staggered reveal for premium UX
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> loadDemandes());
        pause.play();
    }

    private void showSkeletonLoading() {
        cardsContainer.getChildren().clear();
        cardsContainer.setHgap(20);
        cardsContainer.setVgap(20);
        for (int i = 0; i < 4; i++) {
            VBox skeleton = new VBox(12);
            skeleton.setPadding(new Insets(22));
            skeleton.setPrefWidth(300);
            skeleton.setPrefHeight(220);
            skeleton.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 18px; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 18px;");
            skeleton.setOpacity(0.6);

            Region line1 = new Region();
            line1.setPrefHeight(18);
            line1.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 6px;");
            Region line2 = new Region();
            line2.setPrefHeight(12);
            line2.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 4px;");
            Region line3 = new Region();
            line3.setPrefHeight(12);
            line3.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 4px;");
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            Region btnArea = new Region();
            btnArea.setPrefHeight(36);
            btnArea.setPrefWidth(120);
            btnArea.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 12px;");

            skeleton.getChildren().addAll(line1, line2, line3, spacer, btnArea);

            // Skeleton shimmer animation
            FadeTransition shimmer = new FadeTransition(Duration.millis(900), skeleton);
            shimmer.setFromValue(0.4);
            shimmer.setToValue(0.7);
            shimmer.setAutoReverse(true);
            shimmer.setCycleCount(javafx.animation.Animation.INDEFINITE);
            shimmer.setDelay(Duration.millis(i * 150));
            shimmer.play();

            cardsContainer.getChildren().add(skeleton);
        }
    }

    private void loadDemandes() {
        try {
            List<Demande> demandes = demandeService.afficher();
            cardsContainer.getChildren().clear();
            cardIndex = 0;

            if (demandes.isEmpty()) {
                VBox emptyState = new VBox(12);
                emptyState.setAlignment(Pos.CENTER);
                emptyState.setPadding(new Insets(60));
                Label emptyIcon = new Label("?");
                emptyIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: #94a3b8;");
                Label emptyText = new Label("Aucune demande active");
                emptyText.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #64748b; -fx-font-family: 'Segoe UI', sans-serif;");
                Label emptySub = new Label("Les nouvelles demandes apparaîtront ici");
                emptySub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI', sans-serif;");
                emptyState.getChildren().addAll(emptyIcon, emptyText, emptySub);
                cardsContainer.getChildren().add(emptyState);
                return;
            }

            for (Demande d : demandes) {
                VBox card = createCard(d);
                cardsContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int cardIndex = 0;

    private VBox createCard(Demande d) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setPadding(new Insets(22));
        card.setPrefWidth(300);
        card.setPrefHeight(250);
        card.setOpacity(0);
        card.setTranslateY(20);

        boolean isUrgent = "Urgent".equalsIgnoreCase(d.getUrgence());

        card.getStyleClass().add("demande-card");
        if (isUrgent) {
            card.getStyleClass().add("demande-card-urgent");
        }

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), card);
        ft.setToValue(1);
        ft.setDelay(javafx.util.Duration.millis(cardIndex * 80));

        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(400), card);
        tt.setToY(0);
        tt.setDelay(javafx.util.Duration.millis(cardIndex * 80));

        ft.play();
        tt.play();
        cardIndex++;

        Label title = new Label(d.getTitre());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
        title.setWrapText(true);
        
        Label typeAndDesc = new Label("Type: " + d.getType() + "\n\n" + d.getDescription());
        typeAndDesc.setWrapText(true);
        typeAndDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");
        typeAndDesc.setMaxHeight(80);

        Label badge = new Label(d.getUrgence());
        if (isUrgent) {
            badge.getStyleClass().add("badge-urgent");
        } else {
            badge.getStyleClass().add("badge-normal");
        }

        HBox topArea = new HBox(title, new Region(), badge);
        topArea.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(topArea.getChildren().get(1), Priority.ALWAYS);

        Button btnReact = new Button("Répondre");
        btnReact.getStyleClass().add("primary-button");
        btnReact.setOnMouseEntered(e -> {
            btnReact.setScaleX(1.03);
            btnReact.setScaleY(1.03);
        });
        btnReact.setOnMouseExited(e -> {
            btnReact.setScaleX(1.0);
            btnReact.setScaleY(1.0);
        });
        btnReact.setOnMousePressed(e -> {
            btnReact.setScaleX(0.97);
            btnReact.setScaleY(0.97);
        });
        btnReact.setOnMouseReleased(e -> {
            btnReact.setScaleX(1.03);
            btnReact.setScaleY(1.03);
        });
        btnReact.setOnAction(e -> handleReact(d));

        VBox bottomArea = new VBox(btnReact);
        bottomArea.setAlignment(Pos.CENTER);
        
        card.getChildren().addAll(topArea, typeAndDesc, new Region(), bottomArea);
        VBox.setVgrow(card.getChildren().get(2), Priority.ALWAYS);

        return card;
    }

    private void handleReact(Demande d) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/DonReactionView.fxml"));
            Parent root = loader.load();
            
            DonReactionController controller = loader.getController();
            controller.initData(d);

            AideEtdonControllerClientController.getInstance().setView(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRetour() {
        AideEtdonControllerClientController.getInstance().showDons();
    }
}
