package org.example.home.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.user.model.User;
import org.example.user.model.UserRole;
import org.example.utils.AlertUtil;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label avatarLabel;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox dashboardView;

    @FXML
    private Button btnWellbeing;

    @FXML
    private Button btnServicesSociaux;

    @FXML
    private Button btnJournal;

    @FXML
    private Button btnAideEtdon;

    @FXML
    private Button btnCycle;

    @FXML
    private Button btnMonProfil;

    private final List<Button> navButtons = new ArrayList<>();

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
            AlertUtil.showError("Session", "Aucun utilisateur connecté.");
            return;
        }

        setUser(currentUser);

        if (btnWellbeing != null) navButtons.add(btnWellbeing);
        if (btnServicesSociaux != null) navButtons.add(btnServicesSociaux);
        if (btnJournal != null) navButtons.add(btnJournal);
        if (btnAideEtdon != null) navButtons.add(btnAideEtdon);
        if (btnCycle != null) navButtons.add(btnCycle);
        if (btnMonProfil != null) navButtons.add(btnMonProfil);

        configureHomeByRole();
        showDashboard();
    }

    public void setUser(User user) {
        this.currentUser = user;

        if (userNameLabel != null) {
            userNameLabel.setText(user.getNom() + " " + user.getPrenom());
        }

        if (userRoleLabel != null) {
            userRoleLabel.setText(user.getRole().name());
        }

        if (avatarLabel != null) {
            String initials = "";
            if (user.getNom() != null && !user.getNom().isEmpty()) {
                initials += user.getNom().substring(0, 1).toUpperCase();
            }
            if (user.getPrenom() != null && !user.getPrenom().isEmpty()) {
                initials += user.getPrenom().substring(0, 1).toUpperCase();
            }
            avatarLabel.setText(initials);
        }
    }

    private void configureHomeByRole() {
        if (currentUser == null || currentUser.getRole() == null || btnMonProfil == null) {
            return;
        }


    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            SessionManager.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible de retourner à la page Login.");
        }
    }

    @FXML
    private void showDashboard() {
        if (contentArea != null && dashboardView != null) {
            contentArea.getChildren().setAll(dashboardView);
        }
        updateActiveButton(null);
    }

    @FXML
    private void showWellbeing() {
        loadView("/wellbeing/frontoffice/WellbeingClientView.fxml", btnWellbeing);
    }

    @FXML
    private void showServicesSociaux() {
        loadView("/servicesociaux/frontoffice/ServicesSociauxClientView.fxml", btnServicesSociaux);
    }

    @FXML
    private void showAideEtdon() {
        loadView("/aideEtdon/frontoffice/AideEtdonClientView.fxml", btnAideEtdon);
    }

    @FXML
    private void showCycle() {
        loadView("/cycle/frontoffice/CycleClientView.fxml", btnCycle);
    }

    @FXML
    private void showMonProfil() {
        if (currentUser == null || currentUser.getRole() == null) {
            AlertUtil.showWarning("Session", "Utilisateur introuvable.");
            return;
        }

        switch (currentUser.getRole()) {
            case ADMIN:
                loadView("/user/backoffice/UserAdminView.fxml", btnMonProfil);
                break;

            case MEDECIN:
                loadView("/user/frontoffice/UserMedecinView.fxml", btnMonProfil);
                break;

            case PATIENT:
                loadView("/user/frontoffice/UserClientView.fxml", btnMonProfil);
                break;
        }
    }

    private void loadView(String fxmlPath, Button activeBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(view);
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

            contentArea.getChildren().setAll(scrollPane);
            updateActiveButton(activeBtn);

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Chargement", "Impossible de charger : " + fxmlPath);
        }
    }

    private void updateActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("active-nav");
        }

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active-nav");
        }
    }
}