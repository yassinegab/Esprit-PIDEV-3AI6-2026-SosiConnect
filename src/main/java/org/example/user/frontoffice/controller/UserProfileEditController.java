package org.example.user.frontoffice.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.user.model.User;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;
import org.example.utils.ValidationUtil;

import java.sql.SQLException;

public class UserProfileEditController {

    @FXML
    private TextField nomField;
    @FXML
    private TextField prenomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextField ageField;
    @FXML
    private ComboBox<String> sexeComboBox;
    @FXML
    private TextField poidsField;
    @FXML
    private TextField tailleField;
    @FXML
    private TextField handicapField;

    private final ServiceUser serviceUser = new ServiceUser();
    private User user;
    private boolean saved = false;

    @FXML
    public void initialize() {
        sexeComboBox.setItems(FXCollections.observableArrayList("Homme", "Femme", "Autre"));
    }

    public void setUser(User user) {
        this.user = user;

        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telephoneField.setText(user.getTelephone());
        ageField.setText(String.valueOf(user.getAge()));
        sexeComboBox.setValue(user.getSexe());
        poidsField.setText(String.valueOf(user.getPoids()));
        tailleField.setText(String.valueOf(user.getTaille()));
        handicapField.setText(user.getHandicap());
    }

    @FXML
    private void handleSave() {
        try {
            validateFields();

            user.setNom(nomField.getText().trim());
            user.setPrenom(prenomField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setTelephone(telephoneField.getText().trim());
            user.setAge(Integer.parseInt(ageField.getText().trim()));
            user.setSexe(sexeComboBox.getValue());
            user.setPoids(Double.parseDouble(poidsField.getText().trim()));
            user.setTaille(Double.parseDouble(tailleField.getText().trim()));
            user.setHandicap(handicapField.getText().trim());

            serviceUser.updatePatientProfile(user);

            saved = true;
            AlertUtil.showInfo("Profil", "Informations mises à jour avec succès.");
            closeWindow();

        } catch (IllegalArgumentException e) {
            AlertUtil.showWarning("Validation", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors de la mise à jour du profil.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Erreur", "Échec de la modification : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    public boolean isSaved() {
        return saved;
    }

    private void validateFields() {
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
        if (!ValidationUtil.isPositiveInt(ageField.getText())) {
            throw new IllegalArgumentException("Âge invalide.");
        }
        if (!ValidationUtil.isPositiveDouble(poidsField.getText())) {
            throw new IllegalArgumentException("Poids invalide.");
        }
        if (!ValidationUtil.isPositiveDouble(tailleField.getText())) {
            throw new IllegalArgumentException("Taille invalide.");
        }
        if (sexeComboBox.getValue() == null) {
            throw new IllegalArgumentException("Sexe obligatoire.");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}