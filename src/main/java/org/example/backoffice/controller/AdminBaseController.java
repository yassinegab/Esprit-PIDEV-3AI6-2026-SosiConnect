package org.example.backoffice.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AdminBaseController {

    // ✅ Le singleton pointe toujours vers l'instance ACTIVE
    private static AdminBaseController instance;

    @FXML private StackPane adminContentArea;
    @FXML private Button    btnDashboard;
    @FXML private Button    btnUsers;
    @FXML private Button    btnWellbeing;
    @FXML private Button    btnMedical;
    @FXML private Button    btnAide;
    @FXML private Button    btnCycle;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        // ✅ Mettre à jour le singleton à chaque création d'instance
        instance  = this;
        navButtons = Arrays.asList(
                btnDashboard, btnUsers, btnWellbeing,
                btnMedical, btnAide, btnCycle
        );
        showDashboard();
    }

    // ✅ Getter public pour injection directe (évite le problème du singleton périmé)
    public StackPane getAdminContentArea() { return adminContentArea; }

    // ✅ Méthode statique — utilise toujours l'instance courante
    public static void navigateTo(Parent view) {
        if (instance != null && instance.adminContentArea != null) {
            instance.adminContentArea.getChildren().setAll(view);
        } else {
            System.err.println("❌ AdminBaseController instance non disponible.");
        }
    }

    // ✅ Méthode instance pour navigation + highlight bouton (plus fiable)
    public void loadView(String fxmlPath, Button activeBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // ✅ Injecter this dans le controller chargé
            Object ctrl = loader.getController();
            if (ctrl instanceof AdminAware aa) {
                aa.setAdminController(this);
            }

            adminContentArea.getChildren().setAll(view);
            updateActiveButton(activeBtn);

        } catch (Exception e) {
            Label err = new Label(
                    "Impossible de charger : " + fxmlPath + "\n"
                            + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
            );
            err.setStyle("-fx-font-size:13;-fx-text-fill:#ef4444;-fx-padding:20;");
            err.setWrapText(true);
            adminContentArea.getChildren().setAll(err);
            updateActiveButton(activeBtn);
            e.printStackTrace();
        }
    }

    // ── Boutons sidebar ────────────────────────────────────────
    @FXML private void showDashboard() {
        updateActiveButton(btnDashboard);
        Label ph = new Label("Tableau de bord principal");
        ph.setStyle("-fx-font-size:22;-fx-text-fill:#94a3b8;-fx-font-weight:bold;");
        adminContentArea.getChildren().setAll(ph);
    }

    @FXML private void showUserAdmin() {
        loadView("/user/backoffice/UserAdminView.fxml", btnUsers);
    }

    @FXML private void showWellbeingAdmin() {
        loadView("/wellbeing/backoffice/WellbeingAdminDashboard.fxml", btnWellbeing);
    }

    @FXML private void showMedicalAdmin() {
        loadView("/servicesociaux/backoffice/MainMenu.fxml", btnMedical);
    }

    @FXML private void showAideAdmin() {
        loadView("/aideEtdon/backoffice/AideEtdonAdminView.fxml", btnAide);
    }

    @FXML private void showCycleAdmin() {
        loadView("/cycle/backoffice/CycleAdminView.fxml", btnCycle);
    }

    private void updateActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("active");
            btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#475569;");
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active");
            activeBtn.setStyle(
                    "-fx-background-color:#f1f5f9;" +
                            "-fx-text-fill:#6366f1;-fx-font-weight:bold;");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();
            ((Stage) adminContentArea.getScene().getWindow())
                    .getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Interface pour les sous-controllers ───────────────────
    /**
     * Tout controller chargé dans adminContentArea implémente
     * cette interface pour recevoir la référence AdminBaseController.
     * Permet au bouton "← Retour" de recharger MainMenu correctement.
     */
    public interface AdminAware {
        void setAdminController(AdminBaseController admin);
    }
}