package org.example.user.frontoffice.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.user.model.DossierMedical;
import org.example.user.model.User;
import org.example.user.service.ServiceDossierMedical;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class UserClientController {

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
    private Label poidsValueLabel;
    @FXML
    private Label tailleValueLabel;
    @FXML
    private Label sexeValueLabel;
    @FXML
    private Label handicapValueLabel;

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
    private Button editProfileButton;
    @FXML
    private Button chatbotButton;
    @FXML
    private Button addDossierButton;
    @FXML
    private Button editDossierButton;
    @FXML
    private Button deleteDossierButton;

    @FXML
    private PieChart dossierPieChart;
    @FXML
    private BarChart<String, Number> physiqueBarChart;
    @FXML
    private PieChart activitePieChart;

    @FXML
    private VBox dossierContentBox;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ServiceDossierMedical serviceDossierMedical = new ServiceDossierMedical();

    private User currentUser;
    private DossierMedical currentDossier;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
            AlertUtil.showError("Session", "Aucun patient connecté.");
            return;
        }

        loadAllData();
    }

    private void loadAllData() {
        try {
            currentUser = serviceUser.findById(currentUser.getId());
            currentDossier = serviceDossierMedical.findByUserId(currentUser.getId());

            loadProfileData();
            loadDossierData();
            loadCharts();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors du chargement des données patient.");
        }
    }

    private void loadProfileData() {
        fullNameLabel.setText(currentUser.getNom() + " " + currentUser.getPrenom());
        roleLabel.setText("Patient");
        emailLabel.setText(safe(currentUser.getEmail()));
        telephoneLabel.setText(safe(currentUser.getTelephone()));
        ageLabel.setText(currentUser.getAge() + " ans");

        poidsValueLabel.setText(formatDouble(currentUser.getPoids()));
        tailleValueLabel.setText(formatDouble(currentUser.getTaille()));
        sexeValueLabel.setText(safe(currentUser.getSexe()));
        handicapValueLabel.setText(isBlank(currentUser.getHandicap()) ? "Aucun" : currentUser.getHandicap());
    }

    private void loadDossierData() {
        if (currentDossier == null) {
            createdDateLabel.setText("Aucun dossier");
            updatedDateLabel.setText("Aucune mise à jour");

            antecedentsLabel.setText("Aucune donnée");
            maladiesLabel.setText("Aucune donnée");
            allergiesLabel.setText("Aucune donnée");
            traitementsLabel.setText("Aucune donnée");
            diagnosticsLabel.setText("Aucune donnée");
            notesLabel.setText("Aucune donnée");
            objectifLabel.setText("Aucune donnée");
            activiteLabel.setText("Aucune donnée");

            addDossierButton.setVisible(true);
            addDossierButton.setManaged(true);

            editDossierButton.setVisible(false);
            editDossierButton.setManaged(false);

            deleteDossierButton.setVisible(false);
            deleteDossierButton.setManaged(false);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        createdDateLabel.setText("Créé le " + currentDossier.getDateCreation().toLocalDateTime().format(formatter));
        updatedDateLabel.setText("Mis à jour le " + currentDossier.getDerniereMiseAJour().toLocalDateTime().format(formatter));

        antecedentsLabel.setText(displayText(currentDossier.getAntecedentsMedicaux()));
        maladiesLabel.setText(displayText(currentDossier.getMaladiesChroniques()));
        allergiesLabel.setText(displayText(currentDossier.getAllergies()));
        traitementsLabel.setText(displayText(currentDossier.getTraitementsEnCours()));
        diagnosticsLabel.setText(displayText(currentDossier.getDiagnostics()));
        notesLabel.setText(displayText(currentDossier.getNotesMedecin()));
        objectifLabel.setText(displayText(currentDossier.getObjectifSante()));
        activiteLabel.setText(displayText(currentDossier.getNiveauActivite()));

        addDossierButton.setVisible(false);
        addDossierButton.setManaged(false);

        editDossierButton.setVisible(true);
        editDossierButton.setManaged(true);

        deleteDossierButton.setVisible(true);
        deleteDossierButton.setManaged(true);
    }

    private void loadCharts() {
        int filled = 0;
        int empty = 0;

        if (!isBlank(currentDossier != null ? currentDossier.getAntecedentsMedicaux() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getMaladiesChroniques() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getAllergies() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getTraitementsEnCours() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getDiagnostics() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getNotesMedecin() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getObjectifSante() : null)) filled++; else empty++;
        if (!isBlank(currentDossier != null ? currentDossier.getNiveauActivite() : null)) filled++; else empty++;

        dossierPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Renseigné (" + filled + ")", filled),
                new PieChart.Data("Vide (" + empty + ")", empty)
        ));
        dossierPieChart.setTitle("Complétion dossier");
        dossierPieChart.setLabelsVisible(true);

        physiqueBarChart.getData().clear();
        physiqueBarChart.setTitle("Résumé physique");
        physiqueBarChart.setLegendVisible(false);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.getData().add(new XYChart.Data<>("Âge", currentUser.getAge()));
        serie.getData().add(new XYChart.Data<>("Poids", currentUser.getPoids()));
        serie.getData().add(new XYChart.Data<>("Taille", currentUser.getTaille()));
        physiqueBarChart.getData().add(serie);

        for (XYChart.Data<String, Number> data : serie.getData()) {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, new Tooltip(data.getXValue() + " : " + data.getYValue()));
                }
            });
        }

        int actif = 0;
        int modere = 0;
        int faible = 0;

        String niveau = currentDossier != null ? safe(currentDossier.getNiveauActivite()).toLowerCase() : "";

        if (niveau.contains("élevé") || niveau.contains("eleve") || niveau.contains("actif")) {
            actif = 1;
        } else if (niveau.contains("modéré") || niveau.contains("modere")) {
            modere = 1;
        } else {
            faible = 1;
        }

        activitePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Actif (" + actif + ")", actif),
                new PieChart.Data("Modéré (" + modere + ")", modere),
                new PieChart.Data("Faible (" + faible + ")", faible)
        ));
        activitePieChart.setTitle("Niveau d'activité");
        activitePieChart.setLabelsVisible(true);
    }

    @FXML
    private void handleEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/frontoffice/UserProfileEditDialog.fxml"));
            Parent root = loader.load();

            UserProfileEditController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Modifier mon profil");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) {
                loadAllData();
                SessionManager.setCurrentUser(serviceUser.findById(currentUser.getId()));
            }

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Interface", "Impossible d'ouvrir UserProfileEditDialog.fxml");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur après mise à jour du profil.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Erreur", "Le bouton Modifier a échoué : " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/ChatbotDialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Assistant intelligent SOSI");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.setWidth(700);
            stage.setHeight(560);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Chatbot", "Impossible d'ouvrir le chatbot : " + e.getMessage());
        }
    }

    @FXML
    private void handleAddDossier() {
        openDossierForm(null);
    }

    @FXML
    private void handleEditDossier() {
        openDossierForm(currentDossier);
    }

    @FXML
    private void handleDeleteDossier() {
        if (currentDossier == null) {
            AlertUtil.showWarning("Suppression", "Aucun dossier à supprimer.");
            return;
        }

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le dossier médical");
        confirm.setContentText("Voulez-vous vraiment supprimer votre dossier médical ?");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                serviceDossierMedical.delete(currentDossier.getId());
                AlertUtil.showInfo("Suppression", "Dossier médical supprimé avec succès.");
                currentDossier = null;
                loadAllData();
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.showError("Base de données", "Erreur lors de la suppression du dossier médical.");
            }
        }
    }

    private void openDossierForm(DossierMedical dossier) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/frontoffice/DossierMedicalFormDialog.fxml"));
            Parent root = loader.load();

            DossierMedicalFormController controller = loader.getController();
            controller.setData(currentUser.getId(), dossier);

            Stage stage = new Stage();
            stage.setTitle(dossier == null ? "Créer mon dossier médical" : "Modifier mon dossier médical");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) {
                loadAllData();
            }

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Interface", "Impossible d'ouvrir le formulaire du dossier médical.");
        }
    }

    private String displayText(String value) {
        return isBlank(value) ? "Aucune donnée" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }

        return String.format("%.2f", value);
    }
}