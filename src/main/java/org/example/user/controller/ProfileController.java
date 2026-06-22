package org.example.user.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.user.model.User;
import org.example.user.service.ServiceUser;
import org.example.utils.SessionManager;

import java.sql.SQLException;

public class ProfileController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextField ageField;
    @FXML private ChoiceBox<String> sexeBox;
    @FXML private TextField tailleField;
    @FXML private TextField poidsField;
    @FXML private CheckBox handicapBox;
    @FXML private Label messageLabel;

    private ServiceUser serviceUser = new ServiceUser();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            nomField.setText(currentUser.getNom());
            prenomField.setText(currentUser.getPrenom());
            emailField.setText(currentUser.getEmail());
            telephoneField.setText(currentUser.getTelephone());
            ageField.setText(String.valueOf(currentUser.getAge()));
            sexeBox.setValue(currentUser.getSexe());
            tailleField.setText(String.valueOf(currentUser.getTaille()));
            poidsField.setText(String.valueOf(currentUser.getPoids()));
            handicapBox.setSelected(currentUser.isHandicap());
        } else {
            messageLabel.setText("Aucun utilisateur connecté.");
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (currentUser == null) return;

        try {
            // Validate and parse numeric fields
            String ageText = ageField.getText();
            int age = (ageText == null || ageText.trim().isEmpty()) ? 0 : Integer.parseInt(ageText.trim());

            String tailleText = tailleField.getText();
            double taille = (tailleText == null || tailleText.trim().isEmpty()) ? 0.0 : Double.parseDouble(tailleText.trim());

            String poidsText = poidsField.getText();
            double poids = (poidsText == null || poidsText.trim().isEmpty()) ? 0.0 : Double.parseDouble(poidsText.trim());

            // Update user object
            currentUser.setNom(nomField.getText());
            currentUser.setPrenom(prenomField.getText());
            currentUser.setEmail(emailField.getText());
            currentUser.setTelephone(telephoneField.getText());
            currentUser.setAge(age);
            currentUser.setSexe(sexeBox.getValue());
            currentUser.setTaille(taille);
            currentUser.setPoids(poids);
            currentUser.setHandicap(handicapBox.isSelected());

            // Save to database
            serviceUser.modifier(currentUser);
            
            // Update session
            SessionManager.setCurrentUser(currentUser);

            // Show success message
            messageLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            messageLabel.setText("Profil mis à jour avec succès !");
            
            // Optionally, we could trigger HomeController to refresh the avatar initials here if needed

        } catch (NumberFormatException e) {
            messageLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
            messageLabel.setText("Veuillez entrer des nombres valides (Âge, Taille, Poids).");
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
            messageLabel.setText("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }
}
