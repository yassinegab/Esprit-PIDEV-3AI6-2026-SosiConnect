package org.example.user.frontoffice.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.user.model.DossierMedical;
import org.example.user.model.User;
import org.example.user.service.ServiceDossierMedical;
import org.example.utils.AlertUtil;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class PatientDossierViewController {

    @FXML
    private Label patientNameLabel;
    @FXML
    private Label patientEmailLabel;
    @FXML
    private Label patientTelephoneLabel;
    @FXML
    private Label patientAgeSexeLabel;
    @FXML
    private Label patientPoidsLabel;
    @FXML
    private Label patientTailleLabel;
    @FXML
    private Label patientHandicapLabel;

    @FXML
    private Label createdDateLabel;
    @FXML
    private Label updatedDateLabel;

    @FXML
    private Label antecedentsLabel;
    @FXML
    private Label maladiesLabel;
    @FXML
    private Label allergiesLabel;
    @FXML
    private Label traitementsLabel;
    @FXML
    private Label diagnosticsLabel;
    @FXML
    private Label notesLabel;
    @FXML
    private Label objectifLabel;
    @FXML
    private Label activiteLabel;

    @FXML
    private Button editDossierButton;
    @FXML
    private Button createDossierButton;

    private final ServiceDossierMedical serviceDossierMedical = new ServiceDossierMedical();

    private User patient;
    private DossierMedical dossier;

    public void setPatient(User patient) {
        this.patient = patient;
        loadData();
    }

    private void loadData() {
        if (patient == null) {
            AlertUtil.showWarning("Patient", "Aucun patient reçu.");
            return;
        }

        try {
            dossier = serviceDossierMedical.findByUserId(patient.getId());

            patientNameLabel.setText(patient.getNom() + " " + patient.getPrenom());
            patientEmailLabel.setText("✉  " + safe(patient.getEmail()));
            patientTelephoneLabel.setText("📞  " + safe(patient.getTelephone()));
            createdDateLabel.setText("📅  " + patient.getAge() + " ans");
            patientAgeSexeLabel.setText("👤  " + safe(patient.getSexe()));
            patientPoidsLabel.setText("⚖  " + formatDouble(patient.getPoids()) + " kg");
            patientTailleLabel.setText("📏  " + formatDouble(patient.getTaille()) + " cm");
            patientHandicapLabel.setText(display(patient.getHandicap()));

            if (dossier == null) {
                updatedDateLabel.setText("Aucune mise à jour");
                antecedentsLabel.setText("Aucune donnée");
                maladiesLabel.setText("Aucune donnée");
                allergiesLabel.setText("Aucune donnée");
                traitementsLabel.setText("Aucune donnée");
                diagnosticsLabel.setText("Aucune donnée");
                notesLabel.setText("Aucune donnée");
                objectifLabel.setText("Aucune donnée");
                activiteLabel.setText("Aucune donnée");

                createDossierButton.setVisible(true);
                createDossierButton.setManaged(true);
                editDossierButton.setVisible(false);
                editDossierButton.setManaged(false);
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            updatedDateLabel.setText("Mis à jour le " + dossier.getDerniereMiseAJour().toLocalDateTime().format(formatter));

            antecedentsLabel.setText(display(dossier.getAntecedentsMedicaux()));
            maladiesLabel.setText(display(dossier.getMaladiesChroniques()));
            allergiesLabel.setText(display(dossier.getAllergies()));
            traitementsLabel.setText(display(dossier.getTraitementsEnCours()));
            diagnosticsLabel.setText(display(dossier.getDiagnostics()));
            notesLabel.setText(display(dossier.getNotesMedecin()));
            objectifLabel.setText(display(dossier.getObjectifSante()));
            activiteLabel.setText(display(dossier.getNiveauActivite()));

            createDossierButton.setVisible(false);
            createDossierButton.setManaged(false);
            editDossierButton.setVisible(true);
            editDossierButton.setManaged(true);

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur chargement dossier : " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateDossier() {
        openForm(null);
    }

    @FXML
    private void handleEditDossier() {
        openForm(dossier);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) patientNameLabel.getScene().getWindow();
        stage.close();
    }

    private void openForm(DossierMedical dossierToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/frontoffice/DossierMedicalFormDialog.fxml"));
            Parent root = loader.load();

            DossierMedicalFormController controller = loader.getController();
            controller.setData(patient.getId(), dossierToEdit);

            Stage stage = new Stage();
            stage.setTitle(dossierToEdit == null ? "Créer dossier médical" : "Modifier dossier médical");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setWidth(850);
            stage.setHeight(700);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadData();
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Interface", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String display(String value) {
        return value == null || value.trim().isEmpty() ? "Aucune donnée" : value;
    }

    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}