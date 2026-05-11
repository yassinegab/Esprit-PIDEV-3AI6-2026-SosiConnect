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

    private static AdminBaseController instance;

    @FXML private StackPane adminContentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnWellbeing;
    @FXML private Button btnMedical;
    @FXML private Button btnAide;
    @FXML private Button btnAlertes;
    @FXML private Button btnCycle;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        instance = this;
        navButtons = Arrays.asList(btnDashboard, btnUsers, btnWellbeing, btnMedical, btnAide, btnAlertes, btnCycle);
        // Load default view
        showWellbeingAdmin(); 
    }

    public StackPane getAdminContentArea() { return adminContentArea; }

    public static void navigateTo(Parent view) {
        if (instance != null && instance.adminContentArea != null) {
            instance.adminContentArea.getChildren().setAll(view);
        } else {
            System.err.println("❌ AdminBaseController instance non disponible.");
        }
    }

    @FXML
    private void showDashboard() {
        updateActiveButton(btnDashboard);
        Label ph = new Label("Tableau de bord principal");
        ph.setStyle("-fx-font-size:22;-fx-text-fill:#94a3b8;-fx-font-weight:bold;");
        adminContentArea.getChildren().setAll(ph);
    }

    @FXML
    private void showUserAdmin() {
        loadView("/user/backoffice/UserAdminView.fxml", btnUsers);
    }

    @FXML
    private void showWellbeingAdmin() {
        loadView("/wellbeing/backoffice/WellbeingAdminDashboard.fxml", btnWellbeing);
    }

    @FXML
    private void showMedicalAdmin() {
        loadView("/servicesociaux/backoffice/MainMenu.fxml", btnMedical);
    }

    @FXML
    private void showAideAdmin() {
        loadView("/aideEtdon/backoffice/AidesEtDonsAdminView.fxml", btnAide);
    }
    
    @FXML
    private void showAlertesAdmin() {
        loadView("/aideEtdon/backoffice/AideEtdonAlertesAdminView.fxml", btnAlertes);
    }

    @FXML
    private void showCycleAdmin() {
        loadView("/cycle/backoffice/CycleAdminView.fxml", btnCycle);
    }

    public void loadView(String fxmlPath, Button activeBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Inject this into the loaded controller if it's AdminAware
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

    private void updateActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
                btn.setStyle(""); // Clear any inline styles
            }
        }
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("active")) {
                activeBtn.getStyleClass().add("active");
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminContentArea.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface AdminAware {
        void setAdminController(AdminBaseController admin);
    }
}
