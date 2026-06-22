package org.example.aideEtdon.backoffice;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.aideEtdon.model.Alerte;
import org.example.aideEtdon.service.AlerteService;
import org.example.aideEtdon.service.PdfExportService;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AideEtdonAlertesAdminController {

    @FXML private VBox alertesListContainer;
    private AlerteService alerteService;
    private List<Alerte> currentAlertes;

    @FXML
    public void initialize() {
        alerteService = new AlerteService();
        loadAlertes();
    }

    @FXML
    private void loadAlertes() {
        if (alertesListContainer == null) return;
        alertesListContainer.getChildren().clear();
        
        currentAlertes = alerteService.afficher();
        List<Alerte> alertes = currentAlertes;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        int[] index = {0};
        for (Alerte a : alertes) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("alert-row");
            row.setOpacity(0);
            row.setTranslateY(15);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), row);
            ft.setToValue(1);
            ft.setDelay(javafx.util.Duration.millis(index[0] * 60));

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(350), row);
            tt.setToY(0);
            tt.setDelay(javafx.util.Duration.millis(index[0] * 60));

            ft.play();
            tt.play();
            index[0]++;

            VBox infoBox = new VBox(5);
            Label typeLbl = new Label("🚨 URGENCE: " + a.getTypeBesoin());
            typeLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #b91c1c; -fx-font-family: 'Segoe UI', sans-serif;");

            Label dateLbl = new Label("Déclenchée le: " + a.getDateAlerte().format(dtf));
            dateLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");

            Label locLbl = new Label("Localisation: Lat " + a.getLatitude() + " / Lng " + a.getLongitude());
            locLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-family: 'Segoe UI', sans-serif;");

            infoBox.getChildren().addAll(typeLbl, dateLbl, locLbl);

            Label badge = new Label(a.getStatut());
            if ("En Attente".equals(a.getStatut())) {
                badge.getStyleClass().add("warning-badge");
            } else {
                badge.getStyleClass().add("success-badge");
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnResolve = new Button("Marquer Résolue");
            btnResolve.getStyleClass().add("success-admin-button");
            btnResolve.setDisable("Résolue".equals(a.getStatut()));
            btnResolve.setOnAction(e -> {
                alerteService.UPDATE_STATUS(a.getId(), "Résolue");
                loadAlertes();
            });

            Button btnDel = new Button("Supprimer");
            btnDel.getStyleClass().add("danger-button");
            btnDel.setOnAction(e -> {
                alerteService.supprimer(a.getId());
                loadAlertes();
            });

            row.getChildren().addAll(infoBox, spacer, badge, btnResolve, btnDel);
            alertesListContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleExportPdf() {
        if (currentAlertes == null || currentAlertes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Export PDF");
            alert.setHeaderText("Aucune alerte à exporter");
            alert.setContentText("La liste des alertes est vide. Rafraîchissez d'abord la liste.");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        String defaultName = "SosiConnect_Alertes_" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
        fileChooser.setInitialFileName(defaultName);

        File file = fileChooser.showSaveDialog(alertesListContainer.getScene().getWindow());
        if (file == null) return;

        try {
            PdfExportService.exportAlertesToPdf(currentAlertes, file);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Export réussi");
            success.setHeaderText("Rapport PDF généré");
            success.setContentText("Le fichier a été enregistré :\n" + file.getAbsolutePath());
            success.showAndWait();
        } catch (Exception ex) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur d'export");
            error.setHeaderText("Échec de la génération PDF");
            error.setContentText(ex.getMessage());
            error.showAndWait();
            ex.printStackTrace();
        }
    }
}
