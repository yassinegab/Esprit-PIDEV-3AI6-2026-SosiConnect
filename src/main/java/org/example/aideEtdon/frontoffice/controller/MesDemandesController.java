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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.aideEtdon.model.Demande;
import org.example.aideEtdon.model.Don;
import org.example.aideEtdon.service.DemandeService;
import org.example.aideEtdon.service.DonService;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class MesDemandesController {

    @FXML private VBox demandesContainer;
    @FXML private HBox statsRow;

    private DemandeService demandeService;
    private DonService donService;

    @FXML
    public void initialize() {
        demandeService = new DemandeService();
        donService = new DonService();
        loadMyDemandes();
    }

    private void loadMyDemandes() {
        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : 1;

        try {
            List<Demande> allDemandes = demandeService.afficher();

            // Filter to current user's demandes
            List<Demande> myDemandes = allDemandes.stream()
                    .filter(d -> d.getUserId() == currentUserId)
                    .toList();

            // Calculate stats
            int totalDemandes = myDemandes.size();
            int totalResponses = 0;
            int urgentCount = 0;
            for (Demande d : myDemandes) {
                totalResponses += getResponseCount(d.getId());
                if ("Urgent".equalsIgnoreCase(d.getUrgence())) urgentCount++;
            }

            buildStatsRow(totalDemandes, totalResponses, urgentCount);
            displayDemandes(myDemandes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildStatsRow(int total, int responses, int urgent) {
        statsRow.getChildren().clear();
        VBox card1 = createStatCard("📋 Mes demandes", String.valueOf(total), "#2563eb", "#eff6ff");
        VBox card2 = createStatCard("💬 Reponses recues", String.valueOf(responses), "#059669", "#f0fdf4");
        VBox card3 = createStatCard("🚨 Urgentes", String.valueOf(urgent), "#dc2626", "#fef2f2");
        statsRow.getChildren().addAll(card1, card2, card3);

        // Staggered scale entrance for stat cards
        int i = 0;
        for (var node : statsRow.getChildren()) {
            node.setOpacity(0);
            node.setScaleX(0.85);
            node.setScaleY(0.85);

            FadeTransition ft = new FadeTransition(Duration.millis(400), node);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 80));

            ScaleTransition st = new ScaleTransition(Duration.millis(400), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setDelay(Duration.millis(i * 80));

            ft.play();
            st.play();
            i++;
        }
    }

    private VBox createStatCard(String label, String value, String color, String bg) {
        VBox card = new VBox(2);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12; -fx-border-color: " + color + "22; -fx-border-width: 1; -fx-border-radius: 12;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        card.getChildren().addAll(valueLabel, nameLabel);
        return card;
    }

    private void displayDemandes(List<Demande> demandes) {
        demandesContainer.getChildren().clear();

        if (demandes.isEmpty()) {
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(60));
            Label emptyText = new Label("📭 Vous n'avez pas encore cree de demande");
            emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
            Label emptySub = new Label("Cliquez sur '➕ Nouvelle Demande' pour commencer");
            emptySub.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            emptyState.getChildren().addAll(emptyText, emptySub);
            demandesContainer.getChildren().add(emptyState);
            return;
        }

        int idx = 0;
        for (Demande d : demandes) {
            VBox row = createDemandeRow(d, idx++);
            demandesContainer.getChildren().add(row);
        }
    }

    private VBox createDemandeRow(Demande d, int index) {
        VBox row = new VBox(8);
        row.setPadding(new Insets(16));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 1);");
        row.setOpacity(0);
        row.setTranslateX(-20);

        double delay = index * 60;

        FadeTransition ft = new FadeTransition(Duration.millis(400), row);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delay));
        ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), row);
        tt.setToX(0);
        tt.setDelay(Duration.millis(delay));
        tt.play();

        boolean isUrgent = "Urgent".equalsIgnoreCase(d.getUrgence());
        int responseCount = getResponseCount(d.getId());

        // Top: title + badges
        Label title = new Label(d.getTitre());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        title.setWrapText(true);

        String typeEmoji = switch (d.getType() != null ? d.getType() : "") {
            case "Sang" -> "\uD83E\uDE78 ";
            case "Organe" -> "\uD83E\uDEC1 ";
            default -> "\uD83D\uDCE6 ";
        };
        Label typeBadge = new Label(typeEmoji + d.getType());
        typeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10; -fx-background-color: #f0f9ff; -fx-text-fill: #0369a1;");

        Label urgBadge = new Label(isUrgent ? "\uD83D\uDEA8 " + d.getUrgence() : "\u2705 " + d.getUrgence());
        urgBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10; " +
                (isUrgent ? "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626;" : "-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a;"));

        Label respBadge = new Label("\uD83D\uDCE9 " + responseCount + " reponse" + (responseCount > 1 ? "s" : ""));
        respBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10; " +
                (responseCount > 0 ? "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;" : "-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8;"));

        HBox topRow = new HBox(8, title, new Region(), typeBadge, urgBadge, respBadge);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);

        // Description
        Label desc = new Label(d.getDescription());
        desc.setWrapText(true);
        desc.setMaxHeight(40);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Hover effect
        row.setOnMouseEntered(e -> { row.setScaleX(1.01); row.setScaleY(1.01); row.setStyle(row.getStyle().replace("rgba(0,0,0,0.04)", "rgba(13,148,136,0.10)")); });
        row.setOnMouseExited(e -> { row.setScaleX(1.0); row.setScaleY(1.0); row.setStyle(row.getStyle().replace("rgba(13,148,136,0.10)", "rgba(0,0,0,0.04)")); });

        row.getChildren().addAll(topRow, desc);

        // Show responses if any
        if (responseCount > 0) {
            Region divider = new Region();
            divider.setStyle("-fx-background-color: #e2e8f0; -fx-min-height: 1; -fx-max-height: 1;");
            row.getChildren().add(divider);

            Label respHeader = new Label("📨 Reponses recues:");
            respHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
            row.getChildren().add(respHeader);

            try {
                List<Don> responses = donService.afficherParDemande(d.getId());
                for (Don don : responses) {
                    HBox respRow = new HBox(8);
                    respRow.setAlignment(Pos.CENTER_LEFT);
                    respRow.setPadding(new Insets(6, 10, 6, 10));
                    respRow.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");

                    Label dot = new Label("👤");
                    dot.setStyle("-fx-text-fill: #0ea5e9; -fx-font-size: 14px; -fx-font-weight: 900;");

                    Label msgLabel = new Label(don.getMessage());
                    msgLabel.setWrapText(true);
                    msgLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #334155;");
                    HBox.setHgrow(msgLabel, Priority.ALWAYS);

                    String dateStr = "";
                    if (don.getDate() != null) {
                        dateStr = new SimpleDateFormat("dd/MM/yy HH:mm").format(don.getDate());
                    }
                    Label dateLabel = new Label(dateStr);
                    dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

                    respRow.getChildren().addAll(dot, msgLabel, dateLabel);
                    row.getChildren().add(respRow);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return row;
    }

    private int getResponseCount(int demandeId) {
        try {
            return donService.afficherParDemande(demandeId).size();
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML
    public void handleNewDemande() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/DemandeFormView.fxml"));
            Parent root = loader.load();
            AideEtdonControllerClientController.getInstance().setView(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
