package org.example.user.backoffice.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.user.model.User;

public class AdminUserViewController {

    @FXML
    private Label fullNameLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label telephoneLabel;
    @FXML
    private Label ageLabel;
    @FXML
    private Label sexeLabel;
    @FXML
    private Label poidsLabel;
    @FXML
    private Label tailleLabel;
    @FXML
    private Label handicapLabel;

    public void setUser(User user) {
        fullNameLabel.setText(user.getNom() + " " + user.getPrenom());
        roleLabel.setText(formatRole(user.getRole().name()));
        emailLabel.setText(user.getEmail());
        telephoneLabel.setText(user.getTelephone());
        ageLabel.setText(user.getAge() + " ans");
        sexeLabel.setText(user.getSexe());
        poidsLabel.setText(formatDouble(user.getPoids()) + " kg");
        tailleLabel.setText(formatDouble(user.getTaille()) + " cm");
        handicapLabel.setText(user.getHandicap() == null || user.getHandicap().isBlank() ? "Aucun" : user.getHandicap());
    }

    @FXML
    private void handleClose() {
        ((Stage) fullNameLabel.getScene().getWindow()).close();
    }

    private String formatRole(String role) {
        if ("PATIENT".equals(role)) return "Patient";
        if ("MEDECIN".equals(role)) return "Médecin";
        return "Admin";
    }

    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}