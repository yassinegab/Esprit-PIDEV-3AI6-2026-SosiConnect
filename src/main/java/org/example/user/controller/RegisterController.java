package org.example.user.controller;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;

import org.example.user.service.ServiceUser;
import org.example.user.model.User;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField telephoneField;
    @FXML private TextField ageField;
    @FXML private ChoiceBox<String> sexeBox;
    @FXML private TextField tailleField;
    @FXML private TextField poidsField;
    @FXML private ChoiceBox<String> roleBox;
    @FXML private CheckBox handicapBox;
    @FXML private VBox specialiteContainer;
    @FXML private TextField specialiteField;
    @FXML private Button registerButton;

    private ServiceUser serviceUser = new ServiceUser();

    @FXML
    public void initialize() {
        roleBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPro = "Professionnel".equals(newVal);
            specialiteContainer.setVisible(isPro);
            specialiteContainer.setManaged(isPro);
        });
        roleBox.getSelectionModel().select("Client");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            String selectedRole = roleBox.getValue();
            String dbUserRole = "ROLE_PATIENT"; 
            if ("Professionnel".equals(selectedRole)) {
                dbUserRole = "ROLE_MEDECIN"; 
            } else if ("Administrateur".equals(selectedRole)) {
                dbUserRole = "ROLE_ADMIN";
            }

            String ageText = ageField.getText();
            int age = (ageText == null || ageText.trim().isEmpty()) ? 0 : Integer.parseInt(ageText.trim());

            String tailleText = tailleField.getText();
            double taille = (tailleText == null || tailleText.trim().isEmpty()) ? 0.0 : Double.parseDouble(tailleText.trim());

            String poidsText = poidsField.getText();
            double poids = (poidsText == null || poidsText.trim().isEmpty()) ? 0.0 : Double.parseDouble(poidsText.trim());

            User user = new User(
                nomField.getText(),
                prenomField.getText(),
                emailField.getText(),
                passwordField.getText(),
                telephoneField.getText(),
                age,
                sexeBox.getValue(),
                taille,
                poids,
                handicapBox.isSelected(),
                "[\"" + dbUserRole + "\"]", 
                dbUserRole, 
                specialiteField.getText()
            );

            serviceUser.ajouter(user);
            System.out.println("User registered successfully!");
            navigateToLogin(event);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de saisie");
            alert.setHeaderText("Valeur numérique invalide");
            alert.setContentText("Veuillez entrer des nombres valides pour l'âge, la taille et le poids.");
            alert.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de base de données");
            alert.setHeaderText("Erreur lors de l'inscription");
            alert.setContentText("Une erreur est survenue lors de l'enregistrement de l'utilisateur.");
            alert.showAndWait();
        }
    }

    @FXML
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/Login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            FadeTransition ft = new FadeTransition(Duration.millis(500), root);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
            
            stage.getScene().setRoot(root);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
