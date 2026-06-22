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

    private static HomeController instance;

    @FXML private Label avatarLabel;
    @FXML private StackPane contentArea;
    @FXML private Button btnWellbeing;
    @FXML private Button btnServicesSociaux;
    @FXML private Button btnJournal;
    @FXML private Button btnAideEtdon;
    @FXML private Button btnCycle;

    @FXML
    private Button btnMonProfil;

    private final List<Button> navButtons = new ArrayList<>();

    private User currentUser;

    @FXML
    public void initialize() {
        instance = this;
        navButtons = Arrays.asList(btnWellbeing, btnServicesSociaux, btnJournal, btnAideEtdon, btnCycle);
        showDashboard(); // Load the dashboard automatically on init
    }

    public static void navigateTo(Parent view) {
        if (instance != null && instance.contentArea != null) {
            instance.contentArea.getChildren().setAll(view);
        } else {
            System.err.println("HomeController instance non disponible.");
        }
    }

    public void setContent(Parent view) {
        contentArea.getChildren().setAll(view);
    }

    public void setUser(User user) {
        // Dynamic Initials
        String initials = "";
        if (user.getNom() != null && !user.getNom().isEmpty()) initials += user.getNom().substring(0, 1).toUpperCase();
        if (user.getPrenom() != null && !user.getPrenom().isEmpty()) initials += user.getPrenom().substring(0, 1).toUpperCase();
        avatarLabel.setText(initials);

        // Hide Cycle button for male users
        if (user.getSexe() != null && (user.getSexe().equalsIgnoreCase("Homme") || user.getSexe().equalsIgnoreCase("Male"))) {
            if (btnCycle != null) {
                btnCycle.setVisible(false);
                btnCycle.setManaged(false);
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            SessionManager.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) avatarLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible de retourner à la page Login.");
        }
    }

    @FXML
    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home/ClientDashboard.fxml"));
            Parent dashboard = loader.load();
            
            // Set the home controller reference so it can navigate
            Object controller = loader.getController();
            if (controller instanceof ClientDashboardController) {
                ((ClientDashboardController) controller).setHomeController(this);
            }
            
            contentArea.getChildren().setAll(dashboard);
            updateActiveButton(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showProfile() {
        loadView("/user/ProfileView.fxml", null);
    }

    @FXML
    private void showWellbeing() {
        loadView("/wellbeing/frontoffice/WellbeingClientView.fxml", btnWellbeing);
    }

    @FXML
    private void showServicesSociaux() {
        loadView("/servicesociaux/frontoffice/MainMenu.fxml", btnServicesSociaux);
    }

    @FXML
    private void showAideEtdon() {
        loadView("/aideEtdon/frontoffice/AideEtdonClientView.fxml", btnAideEtdon);
    }

    @FXML
    private void showCycle() {
        loadView("/cycle/frontoffice/DisplayCycle.fxml", btnCycle);
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

            // Handle sub-controller injections if needed
            Object controller = loader.getController();
            if (controller instanceof org.example.cycle.frontoffice.controller.DisplayCycleController) {
                ((org.example.cycle.frontoffice.controller.DisplayCycleController) controller).setHomeController(this);
            }

            contentArea.getChildren().setAll(view);
            updateActiveButton(activeBtn);

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Chargement", "Impossible de charger : " + fxmlPath);
        }
    }

    private void updateActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active-nav");
            }
        }

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active-nav");
        }
    }
}