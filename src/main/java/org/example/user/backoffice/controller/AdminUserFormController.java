package org.example.user.backoffice.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.user.model.User;
import org.example.user.model.UserRole;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;
import org.example.utils.ValidationUtil;

public class AdminUserFormController {

    @FXML private Label formTitleLabel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<UserRole> roleCombo;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private TextField handicapField;
    @FXML private Label passwordHintLabel;

    private final ServiceUser serviceUser = new ServiceUser();
    private User editingUser;
    private boolean createMode = true;
    private boolean saved = false;

    private Runnable onSaved;
    private Runnable onCancel;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(UserRole.PATIENT, UserRole.MEDECIN, UserRole.ADMIN));
        sexeCombo.setItems(FXCollections.observableArrayList("Homme", "Femme", "Autre"));
        roleCombo.setValue(UserRole.PATIENT);
        sexeCombo.setValue("Homme");

        roleCombo.setOnAction(e -> updatePatientFields());
        updatePatientFields();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setCreateMode() {
        createMode = true;
        editingUser = null;
        formTitleLabel.setText("Nouvel Utilisateur");
        passwordHintLabel.setText("Mot de passe obligatoire.");
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        roleCombo.setValue(UserRole.PATIENT);
        updatePatientFields();
    }

    public void setEditMode(User user) {
        createMode = false;
        editingUser = user;
        formTitleLabel.setText("Modifier Utilisateur");
        passwordHintLabel.setText("Le mot de passe n'est pas modifié ici.");
        passwordField.setVisible(false);
        passwordField.setManaged(false);

        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telephoneField.setText(user.getTelephone());
        roleCombo.setValue(user.getRole());
        ageField.setText(String.valueOf(user.getAge()));
        sexeCombo.setValue(user.getSexe());
        poidsField.setText(String.valueOf(user.getPoids()));
        tailleField.setText(String.valueOf(user.getTaille()));
        handicapField.setText(user.getHandicap());

        updatePatientFields();
    }

    private void updatePatientFields() {
        boolean isPatient = roleCombo.getValue() == UserRole.PATIENT;

        poidsField.setVisible(isPatient);
        poidsField.setManaged(isPatient);

        tailleField.setVisible(isPatient);
        tailleField.setManaged(isPatient);

        handicapField.setVisible(isPatient);
        handicapField.setManaged(isPatient);

        if (!isPatient) {
            poidsField.setText("0");
            tailleField.setText("0");
            handicapField.clear();
        }
    }

    @FXML
    private void handleSave() {
        try {
            validateFields();

            if (createMode) {
                if (serviceUser.emailExists(emailField.getText().trim())) {
                    throw new IllegalArgumentException("Cet email existe déjà.");
                }

                User user = buildUserFromFields();
                user.setPassword(passwordField.getText().trim());
                serviceUser.add(user);

            } else {
                if (serviceUser.emailExistsForAnotherUser(emailField.getText().trim(), editingUser.getId())) {
                    throw new IllegalArgumentException("Cet email existe déjà pour un autre utilisateur.");
                }

                editingUser.setNom(nomField.getText().trim());
                editingUser.setPrenom(prenomField.getText().trim());
                editingUser.setEmail(emailField.getText().trim());
                editingUser.setTelephone(telephoneField.getText().trim());
                editingUser.setRole(roleCombo.getValue());
                editingUser.setAge(Integer.parseInt(ageField.getText().trim()));
                editingUser.setSexe(sexeCombo.getValue());

                if (roleCombo.getValue() == UserRole.PATIENT) {
                    editingUser.setPoids(Double.parseDouble(poidsField.getText().trim()));
                    editingUser.setTaille(Double.parseDouble(tailleField.getText().trim()));
                    editingUser.setHandicap(handicapField.getText().trim());
                } else {
                    editingUser.setPoids(0);
                    editingUser.setTaille(0);
                    editingUser.setHandicap("");
                }

                serviceUser.updateAdminUser(editingUser);
            }

            saved = true;
            AlertUtil.showInfo("Succès", createMode ? "Utilisateur ajouté avec succès." : "Utilisateur modifié avec succès.");

            if (onSaved != null) {
                onSaved.run();
            }

        } catch (Exception e) {
            AlertUtil.showError("Validation", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private User buildUserFromFields() {
        User user = new User();
        user.setNom(nomField.getText().trim());
        user.setPrenom(prenomField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setTelephone(telephoneField.getText().trim());
        user.setRole(roleCombo.getValue());
        user.setAge(Integer.parseInt(ageField.getText().trim()));
        user.setSexe(sexeCombo.getValue());

        if (roleCombo.getValue() == UserRole.PATIENT) {
            user.setPoids(Double.parseDouble(poidsField.getText().trim()));
            user.setTaille(Double.parseDouble(tailleField.getText().trim()));
            user.setHandicap(handicapField.getText().trim());
        } else {
            user.setPoids(0);
            user.setTaille(0);
            user.setHandicap("");
        }

        return user;
    }

    private void validateFields() {
        if (!ValidationUtil.isTextValid(nomField.getText())) throw new IllegalArgumentException("Nom obligatoire.");
        if (!ValidationUtil.isTextValid(prenomField.getText())) throw new IllegalArgumentException("Prénom obligatoire.");
        if (!ValidationUtil.isEmailValid(emailField.getText())) throw new IllegalArgumentException("Email invalide.");
        if (!ValidationUtil.isPhoneValid(telephoneField.getText())) throw new IllegalArgumentException("Téléphone invalide.");
        if (!ValidationUtil.isPositiveInt(ageField.getText())) throw new IllegalArgumentException("Âge invalide.");
        if (roleCombo.getValue() == null) throw new IllegalArgumentException("Rôle obligatoire.");
        if (sexeCombo.getValue() == null) throw new IllegalArgumentException("Sexe obligatoire.");

        if (roleCombo.getValue() == UserRole.PATIENT) {
            if (!ValidationUtil.isPositiveDouble(poidsField.getText())) throw new IllegalArgumentException("Poids invalide.");
            if (!ValidationUtil.isPositiveDouble(tailleField.getText())) throw new IllegalArgumentException("Taille invalide.");
        }

        if (createMode && !ValidationUtil.isPasswordStrong(passwordField.getText())) {
            throw new IllegalArgumentException("Mot de passe faible. Minimum 8 caractères avec majuscule, minuscule et chiffre.");
        }
    }
}