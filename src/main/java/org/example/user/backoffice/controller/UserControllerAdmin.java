package org.example.user.backoffice.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.user.model.User;
import org.example.user.model.UserRole;
import org.example.user.service.ServiceDossierMedical;
import org.example.user.service.ServiceUser;
import org.example.utils.AlertUtil;

import java.util.List;
import java.util.Optional;

public class UserControllerAdmin {

    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalPatientsLabel;
    @FXML
    private Label totalMedecinsLabel;
    @FXML
    private Label totalDossiersLabel;

    @FXML
    private BarChart<String, Number> ageBarChart;
    @FXML
    private PieChart rolePieChart;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> roleFilterCombo;
    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Number> idColumn;
    @FXML
    private TableColumn<User, String> nomColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> telephoneColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> ageColumn;
    @FXML
    private TableColumn<User, Void> actionsColumn;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ServiceDossierMedical serviceDossierMedical = new ServiceDossierMedical();

    @FXML
    public void initialize() {
        roleFilterCombo.setItems(FXCollections.observableArrayList("TOUS", "PATIENT", "MEDECIN", "ADMIN"));
        roleFilterCombo.setValue("TOUS");

        initTable();
        loadDashboard();
        loadUsers();
    }

    private void initTable() {
        idColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()));

        nomColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNom() + "\n" + data.getValue().getPrenom()));

        emailColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        telephoneColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTelephone()));

        roleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatRole(data.getValue().getRole())));

        ageColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAge() + " ans"));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("👁");
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final ToolBar toolbar = new ToolBar(viewBtn, editBtn, deleteBtn);

            {
                toolbar.getStyleClass().add("action-toolbar");
                viewBtn.getStyleClass().add("action-view-btn");
                editBtn.getStyleClass().add("action-edit-btn");
                deleteBtn.getStyleClass().add("action-delete-btn");

                viewBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openViewDialog(user);
                });

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openEditDialog(user);
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : toolbar);
            }
        });
    }

    private void loadDashboard() {
        try {
            int totalUsers = serviceUser.countAllUsers();
            int totalPatients = serviceUser.countByRole(UserRole.PATIENT);
            int totalMedecins = serviceUser.countByRole(UserRole.MEDECIN);
            int totalDossiers = serviceDossierMedical.countDossiers();

            totalUsersLabel.setText(String.valueOf(totalUsers));
            totalPatientsLabel.setText(String.valueOf(totalPatients));
            totalMedecinsLabel.setText(String.valueOf(totalMedecins));
            totalDossiersLabel.setText(String.valueOf(totalDossiers));

            loadAgeChart();
            loadRoleChart();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Dashboard", "Erreur lors du chargement du dashboard admin : " + e.getMessage());
        }
    }

    private void loadAgeChart() throws Exception {
        ageBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("18-30", serviceUser.countAgeBetween(18, 30)));
        series.getData().add(new XYChart.Data<>("31-45", serviceUser.countAgeBetween(31, 45)));
        series.getData().add(new XYChart.Data<>("46-60", serviceUser.countAgeBetween(46, 60)));
        series.getData().add(new XYChart.Data<>("60+", serviceUser.countAgeGreaterThan(60)));

        ageBarChart.getData().add(series);
    }

    private void loadRoleChart() throws Exception {
        rolePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Patients", serviceUser.countByRole(UserRole.PATIENT)),
                new PieChart.Data("Médecins", serviceUser.countByRole(UserRole.MEDECIN)),
                new PieChart.Data("Admins", serviceUser.countByRole(UserRole.ADMIN))
        ));
    }

    private void loadUsers() {
        try {
            List<User> users = serviceUser.getAll();
            usersTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Utilisateurs", "Erreur lors du chargement des utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearchAndFilter() {
        try {
            String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
            String role = roleFilterCombo.getValue() == null ? "TOUS" : roleFilterCombo.getValue();
            List<User> users = serviceUser.searchUsers(keyword, role);
            usersTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Recherche", "Erreur lors de la recherche : " + e.getMessage());
        }
    }

    @FXML
    private void handleNewUser() {
        openCreateDialog();
    }

    private void openCreateDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/backoffice/AdminUserFormDialog.fxml"));
            Parent root = loader.load();

            AdminUserFormController controller = loader.getController();
            controller.setCreateMode();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvel Utilisateur");
            stage.setScene(new Scene(root));
            stage.setWidth(700);
            stage.setHeight(760);
            stage.showAndWait();

            if (controller.isSaved()) {
                refreshAll();
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Création", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void openEditDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/backoffice/AdminUserFormDialog.fxml"));
            Parent root = loader.load();

            AdminUserFormController controller = loader.getController();
            controller.setEditMode(user);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Utilisateur");
            stage.setScene(new Scene(root));
            stage.setWidth(700);
            stage.setHeight(760);
            stage.showAndWait();

            if (controller.isSaved()) {
                refreshAll();
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Modification", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void openViewDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/backoffice/AdminUserViewDialog.fxml"));
            Parent root = loader.load();

            AdminUserViewController controller = loader.getController();
            controller.setUser(user);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détail Utilisateur");
            stage.setScene(new Scene(root));
            stage.setWidth(700);
            stage.setHeight(620);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Détail", "Impossible d'ouvrir le détail utilisateur : " + e.getMessage());
        }
    }

    private void deleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer utilisateur");
        confirm.setContentText("Voulez-vous supprimer " + user.getNom() + " " + user.getPrenom() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceUser.delete(user.getId());
                AlertUtil.showInfo("Suppression", "Utilisateur supprimé avec succès.");
                refreshAll();
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Suppression", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void refreshAll() {
        loadDashboard();
        handleSearchAndFilter();
    }

    private String formatRole(UserRole role) {
        switch (role) {
            case PATIENT:
                return "Patient";
            case MEDECIN:
                return "Médecin";
            default:
                return "Admin";
        }
    }
}