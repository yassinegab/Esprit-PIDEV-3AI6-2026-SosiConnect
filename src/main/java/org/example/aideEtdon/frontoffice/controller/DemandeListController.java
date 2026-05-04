package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.aideEtdon.model.Demande;
import org.example.aideEtdon.model.Don;
import org.example.aideEtdon.service.DemandeService;
import org.example.aideEtdon.service.DonService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DemandeListController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private Button filterAll;
    @FXML private Button filterSang;
    @FXML private Button filterOrgane;
    @FXML private Button filterAutre;
    @FXML private Button filterUrgent;

    private DemandeService demandeService;
    private DonService donService;
    private List<Demande> allDemandes = new ArrayList<>();
    private String activeFilter = "all";

    private static final String PILL_ACTIVE = "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 5 14; -fx-background-radius: 16; -fx-cursor: hand;";
    private static final String PILL_INACTIVE = "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 14; -fx-background-radius: 16; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand;";
    private static final String PILL_URGENT_ACTIVE = "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 5 14; -fx-background-radius: 16; -fx-cursor: hand;";
    private static final String PILL_URGENT_INACTIVE = "-fx-background-color: #f1f5f9; -fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 14; -fx-background-radius: 16; -fx-border-color: #fecaca; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand;";

    @FXML
    public void initialize() {
        demandeService = new DemandeService();
        donService = new DonService();
        showSkeletonLoading();

        // Live search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> loadDemandes());
        pause.play();
    }

    private void showSkeletonLoading() {
        cardsContainer.getChildren().clear();
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
            allDemandes = demandeService.afficher();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        List<Demande> filtered = new ArrayList<>();

        for (Demande d : allDemandes) {
            // Type filter
            if (!"all".equals(activeFilter)) {
                if ("urgent".equals(activeFilter)) {
                    if (!"Urgent".equalsIgnoreCase(d.getUrgence())) continue;
                } else {
                    if (!activeFilter.equalsIgnoreCase(d.getType())) continue;
                }
            }
            // Search filter
            if (!query.isEmpty()) {
                String searchable = (d.getTitre() + " " + d.getDescription() + " " + d.getType()).toLowerCase();
                if (!searchable.contains(query)) continue;
            }
            filtered.add(d);
        }

        displayCards(filtered);
        resultCountLabel.setText(filtered.size() + " resultat" + (filtered.size() > 1 ? "s" : ""));
    }

    private void displayCards(List<Demande> demandes) {
        cardsContainer.getChildren().clear();
        cardIndex = 0;

        if (demandes.isEmpty()) {
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(60));
            Label emptyIcon = new Label("🔍");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyText = new Label("Aucune demande trouvee");
            emptyText.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #64748b; -fx-font-family: 'Segoe UI', sans-serif;");
            Label emptySub = new Label("Essayez de modifier vos filtres ou votre recherche 🙏");
            emptySub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI', sans-serif;");
            emptyState.getChildren().addAll(emptyIcon, emptyText, emptySub);
            cardsContainer.getChildren().add(emptyState);
            return;
        }

        for (Demande d : demandes) {
            VBox card = createCard(d);
            cardsContainer.getChildren().add(card);
        }
    }

    private void updateFilterStyles() {
        filterAll.setStyle("all".equals(activeFilter) ? PILL_ACTIVE : PILL_INACTIVE);
        filterSang.setStyle("Sang".equals(activeFilter) ? PILL_ACTIVE : PILL_INACTIVE);
        filterOrgane.setStyle("Organe".equals(activeFilter) ? PILL_ACTIVE : PILL_INACTIVE);
        filterAutre.setStyle("Autre".equals(activeFilter) ? PILL_ACTIVE : PILL_INACTIVE);
        filterUrgent.setStyle("urgent".equals(activeFilter) ? PILL_URGENT_ACTIVE : PILL_URGENT_INACTIVE);
    }

    @FXML public void filterAll()    { activeFilter = "all";    updateFilterStyles(); applyFilters(); }
    @FXML public void filterSang()   { activeFilter = "Sang";   updateFilterStyles(); applyFilters(); }
    @FXML public void filterOrgane() { activeFilter = "Organe"; updateFilterStyles(); applyFilters(); }
    @FXML public void filterAutre()  { activeFilter = "Autre";  updateFilterStyles(); applyFilters(); }
    @FXML public void filterUrgent() { activeFilter = "urgent"; updateFilterStyles(); applyFilters(); }

    private int cardIndex = 0;

    private int getResponseCount(int demandeId) {
        try {
            return donService.afficherParDemande(demandeId).size();
        } catch (Exception e) {
            return 0;
        }
    }

    private VBox createCard(Demande d) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setPadding(new Insets(22));
        card.setPrefWidth(300);
        card.setPrefHeight(250);
        card.setOpacity(0);
        card.setTranslateY(30);
        card.setScaleX(0.93);
        card.setScaleY(0.93);

        boolean isUrgent = "Urgent".equalsIgnoreCase(d.getUrgence());

        card.getStyleClass().add("demande-card");
        if (isUrgent) {
            card.getStyleClass().add("demande-card-urgent");
        }

        double delay = cardIndex * 70;

        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delay));

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delay));

        ScaleTransition st = new ScaleTransition(Duration.millis(500), card);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setDelay(Duration.millis(delay));

        ft.play();
        tt.play();
        st.play();
        cardIndex++;

        // Type emoji
        String typeEmoji = switch (d.getType() != null ? d.getType() : "") {
            case "Sang" -> "\uD83E\uDE78";
            case "Organe" -> "\uD83E\uDEC1";
            default -> "\uD83D\uDCE6";
        };

        Label title = new Label(d.getTitre());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
        title.setWrapText(true);

        Label typeLabel = new Label(typeEmoji + " " + d.getType());
        typeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10; -fx-background-color: #f0f9ff; -fx-text-fill: #0369a1;");
        
        Label descLabel = new Label(d.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;");
        descLabel.setMaxHeight(60);

        Label badge = new Label(isUrgent ? "\uD83D\uDEA8 " + d.getUrgence() : "\u2705 " + d.getUrgence());
        if (isUrgent) {
            badge.getStyleClass().add("badge-urgent");
        } else {
            badge.getStyleClass().add("badge-normal");
        }

        HBox topArea = new HBox(title, new Region(), badge);
        topArea.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(topArea.getChildren().get(1), Priority.ALWAYS);

        // Response count badge
        int responseCount = getResponseCount(d.getId());
        Label responseBadge = new Label("\uD83D\uDCE9 " + responseCount + " Reponse" + (responseCount > 1 ? "s" : ""));
        responseBadge.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10; " +
            (responseCount > 0
                ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;"
                : "-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8;")
        );

        Button btnReact = new Button("\uD83D\uDC9A Repondre");
        btnReact.getStyleClass().add("primary-button");
        btnReact.setOnMouseEntered(e -> { btnReact.setScaleX(1.03); btnReact.setScaleY(1.03); });
        btnReact.setOnMouseExited(e -> { btnReact.setScaleX(1.0); btnReact.setScaleY(1.0); });
        btnReact.setOnMousePressed(e -> { btnReact.setScaleX(0.97); btnReact.setScaleY(0.97); });
        btnReact.setOnMouseReleased(e -> { btnReact.setScaleX(1.03); btnReact.setScaleY(1.03); });
        btnReact.setOnAction(e -> handleReact(d));

        HBox bottomArea = new HBox(responseBadge, new Region(), btnReact);
        bottomArea.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bottomArea.getChildren().get(1), Priority.ALWAYS);
        
        card.getChildren().addAll(topArea, typeLabel, descLabel, new Region(), bottomArea);
        VBox.setVgrow(card.getChildren().get(3), Priority.ALWAYS);

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
