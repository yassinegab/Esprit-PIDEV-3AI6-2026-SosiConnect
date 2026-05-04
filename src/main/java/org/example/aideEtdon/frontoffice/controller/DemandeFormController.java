package org.example.aideEtdon.frontoffice.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.aideEtdon.model.Demande;
import org.example.aideEtdon.service.DemandeService;
import org.example.utils.AiService;
import org.example.utils.SessionManager;

public class DemandeFormController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> typeBox;
    @FXML private TextField groupeSanguinField;
    @FXML private TextField organeField;
    @FXML private ComboBox<String> urgenceBox;
    
    @FXML private VBox groupeSanguinContainer;
    @FXML private VBox organeContainer;
    @FXML private Label errorLabel;
    @FXML private Button aiGenerateBtn;
    @FXML private Label aiStatusLabel;

    private DemandeService demandeService;
    private AiService aiService;

    @FXML
    public void initialize() {
        demandeService = new DemandeService();
        aiService = new AiService();

        // Listen for changes in Type to show/hide dynamic fields
        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Sang".equals(newVal)) {
                groupeSanguinContainer.setVisible(true);
                groupeSanguinContainer.setManaged(true);
                organeContainer.setVisible(false);
                organeContainer.setManaged(false);
                organeField.clear();
            } else if ("Organe".equals(newVal)) {
                organeContainer.setVisible(true);
                organeContainer.setManaged(true);
                groupeSanguinContainer.setVisible(false);
                groupeSanguinContainer.setManaged(false);
                groupeSanguinField.clear();
            } else {
                groupeSanguinContainer.setVisible(false);
                groupeSanguinContainer.setManaged(false);
                organeContainer.setVisible(false);
                organeContainer.setManaged(false);
                groupeSanguinField.clear();
                organeField.clear();
            }
        });
    }

    @FXML
    public void handleAiGenerate() {
        String titre = titreField.getText().trim();
        String type = typeBox.getValue();

        if (titre.isEmpty()) {
            showError("✍️ Veuillez d'abord saisir un titre pour generer la description.");
            return;
        }

        // Show loading state
        aiGenerateBtn.setDisable(true);
        aiGenerateBtn.setText("✨ Generation...");
        aiStatusLabel.setText("🤖 L'IA redige votre description...");
        aiStatusLabel.setVisible(true);
        errorLabel.setVisible(false);

        // Build prompt
        String typeInfo = type != null ? " de type " + type : "";
        String groupeInfo = "";
        if ("Sang".equals(type) && !groupeSanguinField.getText().trim().isEmpty()) {
            groupeInfo = ", groupe sanguin " + groupeSanguinField.getText().trim();
        } else if ("Organe".equals(type) && !organeField.getText().trim().isEmpty()) {
            groupeInfo = ", organe: " + organeField.getText().trim();
        }

        String prompt = "Tu es un patient qui a besoin d'aide medicale et qui ecrit une demande sur une plateforme de don. "
                + "Ecris a la premiere personne (je/j'ai besoin) une description sincere et detaillee (3-4 phrases) "
                + "pour ta demande de don" + typeInfo + groupeInfo + ". "
                + "Le titre de ta demande est: \"" + titre + "\". "
                + "Explique ta situation medicale, pourquoi tu as besoin de ce don, et l'urgence de ta situation. "
                + "Sois humain, concret et touchant. Pas de slogan ni de message generique. "
                + "Reponds UNIQUEMENT avec la description, sans guillemets ni prefixe.";

        // Run AI call in background thread
        new Thread(() -> {
            String result = aiService.analyzeText(prompt);
            Platform.runLater(() -> {
                descriptionArea.setText(result);
                aiGenerateBtn.setDisable(false);
                aiGenerateBtn.setText("🤖 Generer avec IA");
                aiStatusLabel.setText("✅ Description generee par IA");
                // Auto-hide status after 3s
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> aiStatusLabel.setVisible(false));
                }).start();
            });
        }).start();
    }

    @FXML
    public void handleValider() {
        errorLabel.setVisible(false);
        String titre = titreField.getText().trim();
        String desc = descriptionArea.getText().trim();
        String type = typeBox.getValue();
        String urgence = urgenceBox.getValue();

        // Validation
        if (titre.isEmpty() || desc.isEmpty() || type == null || urgence == null) {
            showError("⚠️ Veuillez remplir tous les champs obligatoires (Titre, Description, Type, Urgence).");
            return;
        }

        String groupe = null;
        String organe = null;

        if ("Sang".equals(type)) {
            groupe = groupeSanguinField.getText().trim();
            if (groupe.isEmpty()) {
                showError("🩸 Veuillez spécifier le groupe sanguin.");
                return;
            }
        } else if ("Organe".equals(type)) {
            organe = organeField.getText().trim();
            if (organe.isEmpty()) {
                showError("🫁 Veuillez spécifier l'organe.");
                return;
            }
        }

        // Get current user from session
        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : 1;
        Demande d = new Demande(titre, desc, type, groupe, organe, urgence, currentUserId);

        try {
            demandeService.ajouter(d);
            
            // Success, navigate back to home
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("✅ Succès");
            alert.setHeaderText("🎉 Demande créée !");
            alert.setContentText("📝 Votre demande a été créée avec succès ! Les donneurs pourront la voir et y répondre.");
            alert.showAndWait();
            
            handleRetour();
        } catch (Exception e) {
            e.printStackTrace();
            showError("❌ Erreur base de données: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    @FXML
    public void handleRetour() {
        AideEtdonControllerClientController.getInstance().showDons();
    }
}
