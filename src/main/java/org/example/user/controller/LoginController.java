package org.example.user.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.user.model.User;
import org.example.user.model.UserRole;
import org.example.user.service.CaptchaService;
import org.example.user.service.GoogleAuthService;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;
import org.example.utils.RememberMeService;
import org.example.utils.SessionManager;
import org.example.utils.ValidationUtil;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Label errorLabel;

    private final ServiceUser serviceUser = new ServiceUser();
    private final RememberMeService rememberMeService = new RememberMeService();
    private final CaptchaService captchaService = new CaptchaService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        if (rememberMeService.isRemembered()) {
            if (emailField != null) {
                emailField.setText(rememberMeService.getEmail());
            }

            if (rememberMeCheckBox != null) {
                rememberMeCheckBox.setSelected(true);
            }
        } else {
            if (rememberMeCheckBox != null) {
                rememberMeCheckBox.setSelected(false);
            }
        }
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField != null ? emailField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText().trim() : "";

        if (!ValidationUtil.isEmailValid(email)) {
            showError("Email invalide.");
            return;
        }

        if (!ValidationUtil.isTextValid(password)) {
            showError("Mot de passe obligatoire.");
            return;
        }

        if (!captchaService.verifyWithDialog()) {
            showError("Veuillez valider le reCAPTCHA.");
            return;
        }

        try {
            User user = serviceUser.login(email, password);

            if (user == null) {
                showError("Email ou mot de passe incorrect.");
                return;
            }

            handleRememberMe(email);

            SessionManager.setCurrentUser(user);
            openByRole(event, user.getRole());

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de donnees", "Erreur lors de la connexion a la base de donnees.");
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible d'ouvrir la page demandee.");
        }
    }

    @FXML
    void handleGoogleLogin(ActionEvent event) {
        try {
            GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.loginWithGoogle();

            if (!ValidationUtil.isEmailValid(googleUser.getEmail())) {
                showError("Google n'a pas retourne un email valide.");
                return;
            }

            User user = serviceUser.findOrCreateGoogleUser(
                    googleUser.getEmail(),
                    googleUser.getFirstName(),
                    googleUser.getLastName()
            );

            handleRememberMe(user.getEmail());

            SessionManager.setCurrentUser(user);
            openByRole(event, user.getRole());

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Google Auth", e.getMessage());
        }
    }

    private void handleRememberMe(String email) {
        if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
            rememberMeService.save(email);
        } else {
            rememberMeService.clear();
        }
    }

    private void openByRole(ActionEvent event, UserRole role) throws IOException {
        String fxmlPath;

        if (role == UserRole.ADMIN) {
            URL adminLayout = getClass().getResource("/backoffice/AdminLayout.fxml");

            if (adminLayout != null) {
                fxmlPath = "/backoffice/AdminLayout.fxml";
            } else {
                fxmlPath = "/user/backoffice/UserAdminView.fxml";
            }
        } else {
            fxmlPath = "/home/Home.fxml";
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("SOSI Project");
        stage.show();
    }

    @FXML
    void goToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Inscription");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible d'ouvrir la page d'inscription.");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }

        AlertUtil.showWarning("Validation", message);
    }
}