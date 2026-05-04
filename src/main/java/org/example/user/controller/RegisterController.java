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

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> sexeComboBox;

    @FXML private VBox patientFieldsBox;
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private CheckBox handicapCheckBox;
    @FXML private TextField handicapDescriptionField;
    @FXML private CheckBox acceptTermsCheckBox;

    @FXML private TextField verificationCodeField;
    @FXML private Label errorLabel;

    @FXML private VBox stepPersonalBox;
    @FXML private VBox stepPhysicalBox;
    @FXML private VBox stepConfirmBox;
    @FXML private VBox stepCodeBox;

    @FXML private Label stepSubtitleLabel;
    @FXML private Label stepDot1;
    @FXML private Label stepDot2;
    @FXML private Label stepDot3;
    @FXML private Label stepDot4;

    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button sendCodeButton;
    @FXML private Button registerButton;

    private final ServiceUser serviceUser = new ServiceUser();
    private final EmailVerificationService emailVerificationService = new EmailVerificationService();

    private int currentStep = 1;
    private String expectedCode;

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(UserRole.PATIENT, UserRole.MEDECIN, UserRole.ADMIN));
        roleComboBox.setValue(UserRole.PATIENT);

        sexeComboBox.setItems(FXCollections.observableArrayList("Homme", "Femme", "Autre"));
        sexeComboBox.setValue("Homme");

        handicapDescriptionField.setDisable(true);
        errorLabel.setText("");

        updatePatientFields();
        showStep(1);

        roleComboBox.setOnAction(e -> updatePatientFields());
        handicapCheckBox.setOnAction(e -> handicapDescriptionField.setDisable(!handicapCheckBox.isSelected()));
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
        }
    }

    @FXML
    private void handleNextStep() {
        try {
            if (currentStep == 1) {
                validatePersonalStep();
            } else if (currentStep == 2) {
                validatePhysicalStep();
            }

            if (currentStep < 3) {
                showStep(currentStep + 1);
            }

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handlePreviousStep() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    @FXML
    private void handleSendCode() {
        try {
            validatePersonalStep();
            validatePhysicalStep();
            validateConfirmStep();

            if (serviceUser.emailExists(emailField.getText().trim())) {
                showError("Cet email existe déjà.");
                return;
            }

            expectedCode = emailVerificationService.generateCode();
            emailVerificationService.sendVerificationCode(emailField.getText().trim(), expectedCode);

            AlertUtil.showInfo("Code envoyé", "Un code de vérification a été envoyé à votre email.");
            showStep(4);

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors de la vérification de l'email.");
        } catch (MessagingException e) {
            e.printStackTrace();
            AlertUtil.showError("Email", "Impossible d'envoyer le code de vérification.");
        }
    }

    @FXML
    void handleRegister(ActionEvent event) {
        try {
            validateInputs();

            if (expectedCode == null || expectedCode.isBlank()) {
                showError("Veuillez d'abord envoyer le code de vérification.");
                return;
            }

            if (!expectedCode.equals(verificationCodeField.getText().trim())) {
                showError("Code de vérification incorrect.");
                return;
            }

            if (serviceUser.emailExists(emailField.getText().trim())) {
                showError("Cet email existe déjà.");
                return;
            }

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
                user.setHandicap(handicapCheckBox.isSelected() ? handicapDescriptionField.getText().trim() : "");
            } else {
                user.setPoids(0);
                user.setTaille(0);
                user.setHandicap("");
            }

            serviceUser.add(user);

            AlertUtil.showInfo("Succès", "Inscription effectuée avec succès.");
            goToLogin(event);

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors de l’inscription.");
        }
    }

    private void showStep(int step) {
        currentStep = step;

        stepPersonalBox.setVisible(step == 1);
        stepPersonalBox.setManaged(step == 1);

        stepPhysicalBox.setVisible(step == 2);
        stepPhysicalBox.setManaged(step == 2);

        stepConfirmBox.setVisible(step == 3);
        stepConfirmBox.setManaged(step == 3);

        stepCodeBox.setVisible(step == 4);
        stepCodeBox.setManaged(step == 4);

        backButton.setVisible(step > 1);
        backButton.setManaged(step > 1);

        nextButton.setVisible(step < 3);
        nextButton.setManaged(step < 3);

        sendCodeButton.setVisible(step == 3);
        sendCodeButton.setManaged(step == 3);

        registerButton.setVisible(step == 4);
        registerButton.setManaged(step == 4);

        setDotStyle(stepDot1, step == 1);
        setDotStyle(stepDot2, step == 2);
        setDotStyle(stepDot3, step == 3);
        setDotStyle(stepDot4, step == 4);

        if (step == 1) {
            stepSubtitleLabel.setText("Informations personnelles");
        } else if (step == 2) {
            stepSubtitleLabel.setText("Informations physiques");
        } else if (step == 3) {
            stepSubtitleLabel.setText("Confirmation");
        } else {
            stepSubtitleLabel.setText("Vérification email");
        }

        errorLabel.setText("");
    }

    private void setDotStyle(Label dot, boolean active) {
        dot.setStyle(active ? "-fx-text-fill: #dc2626; -fx-font-size: 18px;" : "-fx-text-fill: #d1d5db; -fx-font-size: 18px;");
    }

    private void validateInputs() {
        validatePersonalStep();
        validatePhysicalStep();
        validateConfirmStep();

        if (!ValidationUtil.isTextValid(verificationCodeField.getText())) {
            throw new IllegalArgumentException("Code de vérification obligatoire.");
        }
    }

    private void validatePersonalStep() {
        if (!ValidationUtil.isTextValid(nomField.getText())) {
            throw new IllegalArgumentException("Nom obligatoire.");
        }
        if (!ValidationUtil.isTextValid(prenomField.getText())) {
            throw new IllegalArgumentException("Prénom obligatoire.");
        }
        if (!ValidationUtil.isEmailValid(emailField.getText())) {
            throw new IllegalArgumentException("Email invalide.");
        }
        if (!ValidationUtil.isPhoneValid(telephoneField.getText())) {
            throw new IllegalArgumentException("Téléphone invalide.");
        }
        if (!ValidationUtil.isPasswordStrong(passwordField.getText())) {
            throw new IllegalArgumentException("Mot de passe faible. Minimum 8 caractères avec majuscule, minuscule et chiffre.");
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            throw new IllegalArgumentException("La confirmation du mot de passe est incorrecte.");
        }
        if (roleComboBox.getValue() == null) {
            throw new IllegalArgumentException("Rôle obligatoire.");
        }
    }

    private void validatePhysicalStep() {
        if (!ValidationUtil.isPositiveInt(ageField.getText())) {
            throw new IllegalArgumentException("Âge invalide.");
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
            if (handicapCheckBox.isSelected() && !ValidationUtil.isTextValid(handicapDescriptionField.getText())) {
                throw new IllegalArgumentException("Veuillez préciser le handicap.");
            }
        }
    }

    private void validateConfirmStep() {
        if (!acceptTermsCheckBox.isSelected()) {
            throw new IllegalArgumentException("Vous devez accepter les conditions d’utilisation.");
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
            AlertUtil.showError("Navigation", "Impossible d’ouvrir la page de connexion.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        AlertUtil.showWarning("Validation", message);
    }
}