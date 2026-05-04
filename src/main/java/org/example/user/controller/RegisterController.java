package org.example.user.controller;

import jakarta.mail.MessagingException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.user.model.User;
import org.example.user.model.UserRole;
import org.example.user.service.EmailVerificationService;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;
import org.example.utils.ValidationUtil;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField telephoneField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<UserRole> roleComboBox;

    @FXML
    private TextField ageField;

    @FXML
    private ComboBox<String> sexeComboBox;

    @FXML
    private VBox patientFieldsBox;

    @FXML
    private TextField poidsField;

    @FXML
    private TextField tailleField;

    @FXML
    private CheckBox handicapCheckBox;

    @FXML
    private TextField handicapDescriptionField;

    @FXML
    private CheckBox acceptTermsCheckBox;

    @FXML
    private Label errorLabel;

    private final ServiceUser serviceUser = new ServiceUser();
    private final EmailVerificationService emailVerificationService = new EmailVerificationService();

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(
                UserRole.PATIENT,
                UserRole.MEDECIN,
                UserRole.ADMIN
        ));
        roleComboBox.setValue(UserRole.PATIENT);

        sexeComboBox.setItems(FXCollections.observableArrayList(
                "Homme",
                "Femme",
                "Autre"
        ));
        sexeComboBox.setValue("Homme");

        handicapDescriptionField.setDisable(true);

        if (errorLabel != null) {
            errorLabel.setText("");
        }

        updatePatientFields();

        roleComboBox.setOnAction(e -> updatePatientFields());

        handicapCheckBox.setOnAction(e ->
                handicapDescriptionField.setDisable(!handicapCheckBox.isSelected())
        );
    }

    private void updatePatientFields() {
        boolean isPatient = roleComboBox.getValue() == UserRole.PATIENT;

        patientFieldsBox.setManaged(isPatient);
        patientFieldsBox.setVisible(isPatient);

        if (!isPatient) {
            poidsField.clear();
            tailleField.clear();
            handicapCheckBox.setSelected(false);
            handicapDescriptionField.clear();
            handicapDescriptionField.setDisable(true);
            acceptTermsCheckBox.setSelected(false);
        }
    }

    @FXML
    void handleRegister(ActionEvent event) {
        try {
            validateInputs();

            String email = emailField.getText().trim();

            if (serviceUser.emailExists(email)) {
                showError("Cet email existe deja.");
                return;
            }

            String code = emailVerificationService.generateCode();
            emailVerificationService.sendVerificationCode(email, code);

            if (!emailVerificationService.askCodeAndValidate(code)) {
                showError("Code de verification incorrect.");
                return;
            }

            User user = buildUserFromFields();
            serviceUser.add(user);

            AlertUtil.showInfo("Succes", "Inscription effectuee avec succes.");
            goToLogin(event);

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());

        } catch (MessagingException e) {
            e.printStackTrace();
            AlertUtil.showError(
                    "Email",
                    "Impossible d'envoyer le code de verification : " + e.getMessage()
            );

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de donnees", "Erreur lors de l'inscription.");
        }
    }

    private User buildUserFromFields() {
        User user = new User();

        user.setNom(nomField.getText().trim());
        user.setPrenom(prenomField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setPassword(passwordField.getText().trim());
        user.setTelephone(telephoneField.getText().trim());
        user.setRole(roleComboBox.getValue());
        user.setAge(Integer.parseInt(ageField.getText().trim()));
        user.setSexe(sexeComboBox.getValue());

        if (roleComboBox.getValue() == UserRole.PATIENT) {
            user.setPoids(Double.parseDouble(poidsField.getText().trim()));
            user.setTaille(Double.parseDouble(tailleField.getText().trim()));
            user.setHandicap(
                    handicapCheckBox.isSelected()
                            ? handicapDescriptionField.getText().trim()
                            : ""
            );
        } else {
            user.setPoids(0);
            user.setTaille(0);
            user.setHandicap("");
        }

        return user;
    }

    private void validateInputs() {
        if (!ValidationUtil.isTextValid(nomField.getText())) {
            throw new IllegalArgumentException("Nom obligatoire.");
        }

        if (!ValidationUtil.isTextValid(prenomField.getText())) {
            throw new IllegalArgumentException("Prenom obligatoire.");
        }

        if (!ValidationUtil.isEmailValid(emailField.getText())) {
            throw new IllegalArgumentException("Email invalide.");
        }

        if (!ValidationUtil.isPhoneValid(telephoneField.getText())) {
            throw new IllegalArgumentException("Telephone invalide.");
        }

        if (!ValidationUtil.isPasswordStrong(passwordField.getText())) {
            throw new IllegalArgumentException(
                    "Mot de passe faible. Minimum 8 caracteres avec majuscule, minuscule et chiffre."
            );
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            throw new IllegalArgumentException("La confirmation du mot de passe est incorrecte.");
        }

        if (!ValidationUtil.isPositiveInt(ageField.getText())) {
            throw new IllegalArgumentException("Age invalide.");
        }

        if (roleComboBox.getValue() == null) {
            throw new IllegalArgumentException("Role obligatoire.");
        }

        if (sexeComboBox.getValue() == null) {
            throw new IllegalArgumentException("Sexe obligatoire.");
        }

        if (roleComboBox.getValue() == UserRole.PATIENT) {
            if (!ValidationUtil.isPositiveDouble(poidsField.getText())) {
                throw new IllegalArgumentException("Poids invalide.");
            }

            if (!ValidationUtil.isPositiveDouble(tailleField.getText())) {
                throw new IllegalArgumentException("Taille invalide.");
            }

            if (handicapCheckBox.isSelected()
                    && !ValidationUtil.isTextValid(handicapDescriptionField.getText())) {
                throw new IllegalArgumentException("Veuillez preciser le handicap.");
            }

            if (!acceptTermsCheckBox.isSelected()) {
                throw new IllegalArgumentException("Vous devez accepter les conditions d'utilisation.");
            }
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible d'ouvrir la page de connexion.");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }

        AlertUtil.showWarning("Validation", message);
    }
}