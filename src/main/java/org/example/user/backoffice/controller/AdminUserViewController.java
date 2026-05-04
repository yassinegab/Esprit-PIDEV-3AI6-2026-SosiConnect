package org.example.user.backoffice.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.user.model.User;
import org.example.user.model.UserRole;

public class AdminUserViewController {

    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailLabel;
    @FXML private Label telephoneLabel;
    @FXML private Label ageLabel;
    @FXML private Label sexeLabel;
    @FXML private Label poidsLabel;
    @FXML private Label tailleLabel;
    @FXML private Label handicapLabel;

    private Runnable onClose;

    public void setUser(User user) {
        if (user == null) return;

        fullNameLabel.setText(safe(user.getNom()) + " " + safe(user.getPrenom()));
        roleLabel.setText(formatRole(user.getRole()));

        emailLabel.setText("Email : " + safe(user.getEmail()));
        telephoneLabel.setText("Téléphone : " + safe(user.getTelephone()));
        ageLabel.setText("Âge : " + user.getAge() + " ans");
        sexeLabel.setText("Sexe : " + safe(user.getSexe()));

        boolean isPatient = user.getRole() == UserRole.PATIENT;

        poidsLabel.setVisible(isPatient);
        poidsLabel.setManaged(isPatient);
        tailleLabel.setVisible(isPatient);
        tailleLabel.setManaged(isPatient);
        handicapLabel.setVisible(isPatient);
        handicapLabel.setManaged(isPatient);

        if (isPatient) {
            poidsLabel.setText("Poids : " + formatDouble(user.getPoids()) + " kg");
            tailleLabel.setText("Taille : " + formatDouble(user.getTaille()) + " cm");
            handicapLabel.setText("Handicap : " + (isBlank(user.getHandicap()) ? "Aucun" : user.getHandicap()));
        }
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    private String formatRole(UserRole role) {
        if (role == UserRole.PATIENT) return "Patient";
        if (role == UserRole.MEDECIN) return "Médecin";
        return "Admin";
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatDouble(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }
}