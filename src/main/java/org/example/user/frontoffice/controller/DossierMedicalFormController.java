package org.example.user.frontoffice.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import org.example.user.model.DossierMedical;
import org.example.user.service.ServiceDossierMedical;
import org.example.utils.AlertUtil;

import java.sql.SQLException;

public class DossierMedicalFormController {

    @FXML private TextArea antecedentsArea;
    @FXML private TextArea maladiesArea;
    @FXML private TextArea allergiesArea;
    @FXML private TextArea traitementsArea;
    @FXML private TextArea diagnosticsArea;
    @FXML private TextArea notesArea;
    @FXML private TextArea objectifArea;
    @FXML private ComboBox<String> activiteComboBox;

    private final ServiceDossierMedical serviceDossierMedical = new ServiceDossierMedical();

    private DossierMedical dossier;
    private int userId;
    private boolean saved = false;

    private Runnable onSaved;
    private Runnable onCancel;

    @FXML
    public void initialize() {
        activiteComboBox.getItems().addAll("Faible", "Modéré", "Élevé");
        activiteComboBox.setValue("Faible");
    }

    public void setData(int userId, DossierMedical dossier) {
        this.userId = userId;
        this.dossier = dossier;

        if (dossier != null) {
            antecedentsArea.setText(dossier.getAntecedentsMedicaux());
            maladiesArea.setText(dossier.getMaladiesChroniques());
            allergiesArea.setText(dossier.getAllergies());
            traitementsArea.setText(dossier.getTraitementsEnCours());
            diagnosticsArea.setText(dossier.getDiagnostics());
            notesArea.setText(dossier.getNotesMedecin());
            objectifArea.setText(dossier.getObjectifSante());
            activiteComboBox.setValue(dossier.getNiveauActivite() == null || dossier.getNiveauActivite().isBlank()
                    ? "Faible"
                    : dossier.getNiveauActivite());
        }
    }

    @FXML
    private void handleSave() {
        try {
            if (dossier == null) {
                dossier = new DossierMedical();
                dossier.setUserId(userId);
            }

            dossier.setAntecedentsMedicaux(antecedentsArea.getText().trim());
            dossier.setMaladiesChroniques(maladiesArea.getText().trim());
            dossier.setAllergies(allergiesArea.getText().trim());
            dossier.setTraitementsEnCours(traitementsArea.getText().trim());
            dossier.setDiagnostics(diagnosticsArea.getText().trim());
            dossier.setNotesMedecin(notesArea.getText().trim());
            dossier.setObjectifSante(objectifArea.getText().trim());
            dossier.setNiveauActivite(activiteComboBox.getValue());

            if (dossier.getId() == 0) {
                serviceDossierMedical.add(dossier);
                AlertUtil.showInfo("Dossier médical", "Dossier médical créé avec succès.");
            } else {
                serviceDossierMedical.update(dossier);
                AlertUtil.showInfo("Dossier médical", "Dossier médical mis à jour avec succès.");
            }

            saved = true;

            if (onSaved != null) {
                onSaved.run();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors de l'enregistrement du dossier médical.");
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

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }
}
