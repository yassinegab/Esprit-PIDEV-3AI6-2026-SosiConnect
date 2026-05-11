package org.example.cycle.frontoffice.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.cycle.model.IntensiteSymptome;
import org.example.cycle.model.Symptome;
import org.example.cycle.model.TypeSymptome;
import org.example.cycle.service.SymptomeService;
import org.example.home.controller.HomeController;
import org.example.utils.AlertHelper;
import org.example.utils.SessionManager;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DisplaySymptomeController {

    @FXML private FlowPane symptomeContainer;
    @FXML private Button btnAddSymptom;
    
    // Integrated Form Fields
    @FXML private ComboBox<TypeSymptome> typeComboBox;
    @FXML private ComboBox<IntensiteSymptome> intensiteComboBox;
    @FXML private DatePicker datePicker;

    @FXML private Label lblPhase;
    @FXML private Label lblAlimentation;
    @FXML private Label lblSport;
    @FXML private Label lblBienEtre;
    @FXML private Label lblResume;
    @FXML private VBox adviceContainer;

    private ObservableList<Symptome> symptomes = FXCollections.observableArrayList();
    private HomeController homeController;
    private int currentCycleId = -1;
    private final SymptomeService symptomeService = new SymptomeService();

    private static JSONObject cachedAdvice = null;
    private static LocalDate cacheDate = null;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    public void setCycleId(int cycleId) {
        this.currentCycleId = cycleId;
        loadSymptomes();
    }

    @FXML
    public void initialize() {
        // Initialize Form
        typeComboBox.setItems(FXCollections.observableArrayList(TypeSymptome.values()));
        intensiteComboBox.setItems(FXCollections.observableArrayList(IntensiteSymptome.values()));
        datePicker.setValue(LocalDate.now());

        if (currentCycleId == -1) {
             loadSymptomes();
             loadSmartAdvice();
        }
    }

    @FXML
    private void handleInlineAddSymptome() {
        if (typeComboBox.getValue() == null || intensiteComboBox.getValue() == null || datePicker.getValue() == null) {
            AlertHelper.showErrorAlert("Erreur de Saisie", "Veuillez remplir tous les champs du formulaire.");
            return;
        }

        int cycleIdToUse = currentCycleId;
        if (cycleIdToUse == -1) {
            cycleIdToUse = symptomeService.getLastInsertedCycleId();
        }

        if (cycleIdToUse == -1) {
            AlertHelper.showErrorAlert("Erreur", "Aucun cycle trouvé. Créez un cycle d'abord.");
            return;
        }

        Symptome newSymptome = new Symptome(
                cycleIdToUse,
                typeComboBox.getValue(),
                intensiteComboBox.getValue(),
                Date.valueOf(datePicker.getValue())
        );

        try {
            symptomeService.ajouter(newSymptome);
            AlertHelper.showSuccessAlert("Succès", "Symptôme ajouté !");
            
            // Clear form
            typeComboBox.setValue(null);
            intensiteComboBox.setValue(null);
            datePicker.setValue(LocalDate.now());
            
            // Refresh list
            loadSymptomes();
        } catch (SQLException e) {
            AlertHelper.showErrorAlert("Erreur technique", e.getMessage());
        }
    }

    private void loadSmartAdvice() {
        if (cacheDate != null && cacheDate.isEqual(LocalDate.now()) && cachedAdvice != null) {
            updateAdviceUI(cachedAdvice);
            return;
        }

        lblPhase.setText("Phase : Chargement de l'analyse IA...");
        
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8082/cycle/advice"))
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    cachedAdvice = jsonResponse;
                    cacheDate = LocalDate.now();
                    Platform.runLater(() -> updateAdviceUI(jsonResponse));
                } else {
                    Platform.runLater(() -> lblPhase.setText("Erreur lors de la génération des conseils."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblPhase.setText("Service IA indisponible."));
            }
        }).start();
    }

    private void updateAdviceUI(JSONObject jsonResponse) {
        if (jsonResponse.has("error")) {
            lblPhase.setText("Erreur IA : " + jsonResponse.getString("error"));
            return;
        }
        
        String phase = jsonResponse.optString("phase", "Inconnue");
        int jourCycle = jsonResponse.optInt("jourCycle", 0);
        
        lblPhase.setText("Phase actuelle : " + phase + " (Jour " + jourCycle + ")");
        
        JSONObject advice = jsonResponse.optJSONObject("advice");
        if (advice != null) {
            lblAlimentation.setText(advice.optString("alimentation", "N/A"));
            lblSport.setText(advice.optString("sport", "N/A"));
            lblBienEtre.setText(advice.optString("bienEtre", "N/A"));
            lblResume.setText(advice.optString("resume", "N/A"));
        }
    }

    public void loadSymptomes() {
        symptomeContainer.getChildren().clear();
        symptomes.clear();

        try {
            List<Symptome> fetchedSymptomes = new ArrayList<>();
            if (currentCycleId != -1) {
                fetchedSymptomes = symptomeService.getSymptomesByCycleId(currentCycleId);
            } else {
                org.example.user.model.User currentUser = SessionManager.getCurrentUser();
                if (currentUser != null) {
                    List<org.example.cycle.model.Cycle> userCycles = new org.example.cycle.service.CycleService().getCyclesByUserId(currentUser.getId());
                    for (org.example.cycle.model.Cycle c : userCycles) {
                        fetchedSymptomes.addAll(symptomeService.getSymptomesByCycleId(c.getCycle_id()));
                    }
                }
            }
            symptomes.addAll(fetchedSymptomes);

            for (Symptome s : symptomes) {
                VBox card = new VBox();
                card.setSpacing(10);
                card.setPrefWidth(250);
                card.getStyleClass().add("cycle-card"); 

                Label typeLabel = new Label("Type");
                typeLabel.getStyleClass().add("cycle-label-title");
                Label typeValue = new Label(s.getType().name());
                typeValue.getStyleClass().add("cycle-label-value");

                Label intensityLabel = new Label("Intensité");
                intensityLabel.getStyleClass().add("cycle-label-title");
                Label intensityValue = new Label(s.getIntensite().name());
                intensityValue.getStyleClass().add("cycle-label-value");

                Label dateLabel = new Label("Date d'observation");
                dateLabel.getStyleClass().add("cycle-label-title");
                Label dateValue = new Label(s.getDateObservation().toString());
                dateValue.getStyleClass().add("cycle-label-value");

                Button editBtn = new Button("Edit");
                editBtn.getStyleClass().add("btn-edit");
                editBtn.setOnAction(e -> goToEdit(s));

                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().add("btn-delete");
                deleteBtn.setOnAction(e -> {
                    boolean confirmed = AlertHelper.showConfirmationAlert("Confirmation de Suppression", "Êtes-vous sûr ?");
                    if (confirmed) {
                        try {
                            symptomeService.supprimer(s.getIdSymptome());
                            AlertHelper.showSuccessAlert("Succès", "Symptôme supprimé.");
                            loadSymptomes();
                        } catch (SQLException ex) {
                            AlertHelper.showErrorAlert("Erreur", ex.getMessage());
                        }
                    }
                });

                HBox buttons = new HBox();
                buttons.getStyleClass().add("cycle-buttons");
                buttons.getChildren().addAll(editBtn, deleteBtn);

                card.getChildren().addAll(typeLabel, typeValue, intensityLabel, intensityValue, dateLabel, dateValue, buttons);
                symptomeContainer.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void goToEdit(Symptome symptome) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cycle/frontoffice/edit_symptome.fxml"));
            Parent view = loader.load();
            EditSymptomeController controller = loader.getController();
            controller.setHomeController(homeController);
            controller.setSymptome(symptome);
            if (homeController != null) homeController.setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAddSymptom() {
        // Now integrated, but we keep the method if needed or handle the button differently
    }
}
