package org.example.user.frontoffice.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.user.model.User;
import org.example.user.service.ServiceDossierMedical;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class UserMedecinController {

    @FXML
    private Label totalPatientsLabel;
    @FXML
    private Label totalDossiersLabel;
    @FXML
    private Label totalMaladiesLabel;
    @FXML
    private Label totalAllergiesLabel;

    @FXML
    private Label ageMoyenLabel;
    @FXML
    private Label poidsMoyenLabel;
    @FXML
    private Label hommesFemmesLabel;
    @FXML
    private Label handicapLabel;

    @FXML
    private BarChart<String, Number> activiteBarChart;

    @FXML
    private TextField searchField;
    @FXML
    private Label countPatientsLabel;

    @FXML
    private TableView<User> patientsTable;
    @FXML
    private TableColumn<User, Number> idColumn;
    @FXML
    private TableColumn<User, String> nomColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> telephoneColumn;
    @FXML
    private TableColumn<User, String> ageColumn;
    @FXML
    private TableColumn<User, String> sexeColumn;
    @FXML
    private TableColumn<User, Void> dossierColumn;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ServiceDossierMedical serviceDossierMedical = new ServiceDossierMedical();

    @FXML
    public void initialize() {
        initTable();
        loadDashboard();
        loadPatients();
    }

    private void initTable() {
        idColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()));

        nomColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNom() + " " + data.getValue().getPrenom()));

        emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getEmail())));

        telephoneColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getTelephone())));

        ageColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAge() + " ans"));

        sexeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getSexe())));

        dossierColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Voir");

            {
                btn.getStyleClass().add("voir-button");
                btn.setOnAction(event -> {
                    User patient = getTableView().getItems().get(getIndex());
                    openPatientDossier(patient);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadDashboard() {
        try {
            List<User> patients = serviceUser.getPatients();

            int totalPatients = patients.size();
            int totalDossiers = serviceDossierMedical.countDossiers();
            int totalMaladies = serviceDossierMedical.countPatientsWithChronicDiseases();
            int totalAllergies = serviceDossierMedical.countPatientsWithAllergies();

            totalPatientsLabel.setText(String.valueOf(totalPatients));
            totalDossiersLabel.setText(String.valueOf(totalDossiers));
            totalMaladiesLabel.setText(String.valueOf(totalMaladies));
            totalAllergiesLabel.setText(String.valueOf(totalAllergies));

            double ageMoyen = patients.stream().mapToInt(User::getAge).average().orElse(0);
            double poidsMoyen = patients.stream().mapToDouble(User::getPoids).average().orElse(0);

            long hommes = patients.stream()
                    .filter(u -> u.getSexe() != null && u.getSexe().equalsIgnoreCase("Homme"))
                    .count();

            long femmes = patients.stream()
                    .filter(u -> u.getSexe() != null && u.getSexe().equalsIgnoreCase("Femme"))
                    .count();

            long handicap = patients.stream()
                    .filter(u -> u.getHandicap() != null && !u.getHandicap().trim().isEmpty())
                    .count();

            ageMoyenLabel.setText((int) Math.round(ageMoyen) + " ans");
            poidsMoyenLabel.setText((int) Math.round(poidsMoyen) + " kg");
            hommesFemmesLabel.setText(hommes + " / " + femmes);
            handicapLabel.setText(String.valueOf(handicap));

            loadBarChart();

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors du chargement du dashboard médecin : " + e.getMessage());
        }
    }

    private void loadBarChart() throws SQLException {
        activiteBarChart.getData().clear();

        Map<String, Integer> stats = serviceDossierMedical.getActiviteStats();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Patients");
        series.getData().add(new XYChart.Data<>("Sédentaire", stats.getOrDefault("Sédentaire", 0)));
        series.getData().add(new XYChart.Data<>("Léger", stats.getOrDefault("Léger", 0)));
        series.getData().add(new XYChart.Data<>("Modéré", stats.getOrDefault("Modéré", 0)));
        series.getData().add(new XYChart.Data<>("Actif", stats.getOrDefault("Actif", 0)));
        series.getData().add(new XYChart.Data<>("Très actif", stats.getOrDefault("Très actif", 0)));

        activiteBarChart.getData().add(series);
    }

    private void loadPatients() {
        try {
            List<User> patients = serviceUser.getPatients();
            patientsTable.setItems(FXCollections.observableArrayList(patients));
            countPatientsLabel.setText(patients.size() + " patient(s)");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Base de données", "Erreur lors du chargement des patients : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();

        try {
            List<User> patients;
            if (keyword.isEmpty()) {
                patients = serviceUser.getPatients();
            } else {
                patients = serviceUser.searchPatientsByName(keyword);
            }

            patientsTable.setItems(FXCollections.observableArrayList(patients));
            countPatientsLabel.setText(patients.size() + " patient(s)");

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Recherche", "Erreur lors de la recherche : " + e.getMessage());
        }
    }

    private void openPatientDossier(User patient) {
        if (patient == null) {
            AlertUtil.showWarning("Patient", "Aucun patient sélectionné.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/frontoffice/PatientDossierViewDialog.fxml"));
            Parent root = loader.load();

            PatientDossierViewController controller = loader.getController();
            controller.setPatient(patient);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Dossier médical - " + patient.getNom() + " " + patient.getPrenom());
            stage.setScene(new Scene(root));
            stage.setWidth(1000);
            stage.setHeight(700);
            stage.showAndWait();

            loadDashboard();
            loadPatients();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur ouverture dossier");
            alert.setHeaderText("Le bouton Voir a échoué");
            alert.setContentText(
                    "Message : " + e.getClass().getSimpleName() + "\n" +
                            (e.getMessage() == null ? "Aucun détail" : e.getMessage())
            );
            alert.showAndWait();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}