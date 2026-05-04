package org.example.backoffice.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.utils.AlertUtil;
import org.example.utils.SessionManager;

import java.io.IOException;

public class AdminBaseController {

    @FXML
    private StackPane adminContentArea;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnUsers;

    @FXML
    private Button btnWellbeing;

    @FXML
    private Button btnMedical;

    @FXML
    private Button btnAide;

    @FXML
    private Button btnCycle;

    @FXML
    public void initialize() {
        showUserAdmin();
    }

    @FXML
    private void showDashboard() {
        loadContent("/user/backoffice/UserAdminView.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void showUserAdmin() {
        loadContent("/user/backoffice/UserAdminView.fxml");
        setActiveButton(btnUsers);
    }

    @FXML
    private void showWellbeingAdmin() {
        loadContent("/wellbeing/backoffice/WellbeingAdminView.fxml");
        setActiveButton(btnWellbeing);
    }

    @FXML
    private void showMedicalAdmin() {
        loadContent("/servicesociaux/backoffice/ServicesSociauxAdminView.fxml");
        setActiveButton(btnMedical);
    }

    @FXML
    private void showAideAdmin() {
        loadContent("/aideEtdon/backoffice/AideEtdonAdminView.fxml");
        setActiveButton(btnAide);
    }

    @FXML
    private void showCycleAdmin() {
        loadContent("/cycle/backoffice/CycleAdminView.fxml");
        setActiveButton(btnCycle);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            SessionManager.clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) adminContentArea.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Connexion");

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible de retourner à la page Login.");
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(false);
            scrollPane.setPannable(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            scrollPane.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-background: transparent;" +
                            "-fx-border-color: transparent;"
            );

            adminContentArea.getChildren().setAll(scrollPane);

        } catch (IOException e) {
            e.printStackTrace();

            Label errorLabel = new Label("Impossible de charger : " + fxmlPath);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16px; -fx-font-weight: bold;");
            adminContentArea.getChildren().setAll(errorLabel);
        }
    }

    private void setActiveButton(Button activeButton) {
        resetButtonStyle(btnDashboard);
        resetButtonStyle(btnUsers);
        resetButtonStyle(btnWellbeing);
        resetButtonStyle(btnMedical);
        resetButtonStyle(btnAide);
        resetButtonStyle(btnCycle);

        if (activeButton != null) {
            activeButton.setStyle(
                    "-fx-background-color: #dc2626;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 12 18;" +
                            "-fx-font-weight: bold;" +
                            "-fx-alignment: CENTER_LEFT;"
            );
        }
    }

    private void resetButtonStyle(Button button) {
        if (button != null) {
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #334155;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 12 18;" +
                            "-fx-font-weight: normal;" +
                            "-fx-alignment: CENTER_LEFT;"
            );
        }
    }
}