package org.example.aideEtdon.frontoffice.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import org.example.aideEtdon.model.Demande;
import org.example.aideEtdon.model.Don;
import org.example.aideEtdon.service.DonService;
import org.example.utils.AiService;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.util.*;

public class DonReactionController {

    @FXML private Label demandeTitleLabel;
    @FXML private Label demandeDescLabel;
    @FXML private Label errorLabel;
    @FXML private Label urgenceBadge;
    @FXML private Label typeInfoLabel;
    @FXML private Label bloodGroupLabel;
    @FXML private Label organeInfoLabel;
    @FXML private Label compatibilityLabel;
    @FXML private Label bloodMatchLabel;
    @FXML private Label messageStepLabel;
    @FXML private Label contactStepLabel;
    @FXML private HBox bloodGroupRow;
    @FXML private HBox organeRow;
    @FXML private HBox quickActionsRow;
    @FXML private VBox compatibilityBox;
    @FXML private VBox donorBloodSection;
    @FXML private TextArea messageArea;
    @FXML private ComboBox<String> donorBloodGroup;
    @FXML private CheckBox shareContactCheck;
    @FXML private CheckBox availableNowCheck;
    @FXML private Button aiSuggestBtn;
    @FXML private Label aiStatusLabel;

    private Demande demande;
    private DonService donService;
    private AiService aiService;
    private String selectedQuickAction = null;

    // Blood compatibility matrix (receiver -> compatible donors)
    private static final Map<String, List<String>> BLOOD_MATRIX = new HashMap<>();
    static {
        BLOOD_MATRIX.put("A+",  Arrays.asList("A+", "A-", "O+", "O-"));
        BLOOD_MATRIX.put("O+",  Arrays.asList("O+", "O-"));
        BLOOD_MATRIX.put("B+",  Arrays.asList("B+", "B-", "O+", "O-"));
        BLOOD_MATRIX.put("AB+", Arrays.asList("A+", "O+", "B+", "AB+", "A-", "O-", "B-", "AB-"));
        BLOOD_MATRIX.put("A-",  Arrays.asList("A-", "O-"));
        BLOOD_MATRIX.put("O-",  Arrays.asList("O-"));
        BLOOD_MATRIX.put("B-",  Arrays.asList("B-", "O-"));
        BLOOD_MATRIX.put("AB-", Arrays.asList("AB-", "A-", "B-", "O-"));
    }

    @FXML
    public void initialize() {
        donService = new DonService();
        aiService = new AiService();
    }

    public void initData(Demande d) {
        this.demande = d;
        demandeTitleLabel.setText(d.getTitre());
        demandeDescLabel.setText(d.getDescription());
        typeInfoLabel.setText(d.getType());

        // Urgency badge
        if ("Urgent".equalsIgnoreCase(d.getUrgence())) {
            urgenceBadge.setText("🚨 URGENT");
            urgenceBadge.setStyle(urgenceBadge.getStyle() + "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-border-color: #fecaca; -fx-border-width: 1; -fx-border-radius: 12;");
        } else {
            urgenceBadge.setText("✅ Normal");
            urgenceBadge.setStyle(urgenceBadge.getStyle() + "-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a; -fx-border-color: #bbf7d0; -fx-border-width: 1; -fx-border-radius: 12;");
        }

        // Type-specific fields
        if ("Sang".equalsIgnoreCase(d.getType())) {
            bloodGroupRow.setVisible(true);
            bloodGroupRow.setManaged(true);
            bloodGroupLabel.setText(d.getGroupeSanguin() != null ? d.getGroupeSanguin() : "Non specifie");

            // Show donor blood group section
            donorBloodSection.setVisible(true);
            donorBloodSection.setManaged(true);
            messageStepLabel.setText("3");
            contactStepLabel.setText("4");

            // Compatibility check on selection
            donorBloodGroup.setOnAction(e -> checkBloodCompatibility());

            // Show compatible groups info
            List<String> compatibles = BLOOD_MATRIX.getOrDefault(
                    d.getGroupeSanguin() != null ? d.getGroupeSanguin().toUpperCase() : "", new ArrayList<>());
            if (!compatibles.isEmpty()) {
                compatibilityBox.setVisible(true);
                compatibilityBox.setManaged(true);
                compatibilityLabel.setText("🩸 Groupes compatibles: " + String.join(", ", compatibles));
            }
        } else if ("Organe".equalsIgnoreCase(d.getType())) {
            organeRow.setVisible(true);
            organeRow.setManaged(true);
            organeInfoLabel.setText(d.getOrgane() != null ? d.getOrgane() : "Non specifie");
        }

        // Build quick action buttons based on type
        buildQuickActions(d);
    }

    private void buildQuickActions(Demande d) {
        quickActionsRow.getChildren().clear();

        List<String> actions = new ArrayList<>();
        if ("Sang".equalsIgnoreCase(d.getType())) {
            actions.add("🩸 Je peux donner du sang");
            actions.add("🏅 Je suis un donneur regulier");
            actions.add("🤝 Je connais un donneur");
        } else if ("Organe".equalsIgnoreCase(d.getType())) {
            actions.add("💚 Je suis volontaire");
            actions.add("💬 Je souhaite en savoir plus");
            actions.add("🤝 Je connais un donneur");
        } else {
            actions.add("❤️ Je peux aider");
            actions.add("⏰ Je suis disponible");
            actions.add("👥 Je connais quelqu'un");
        }

        for (String action : actions) {
            Button btn = new Button(action);
            btn.setStyle(
                "-fx-background-color: #f8fafc; -fx-text-fill: #334155; -fx-font-size: 11px; " +
                "-fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 20; " +
                "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-border-radius: 20; -fx-cursor: hand;"
            );
            btn.setOnAction(e -> selectQuickAction(btn, action));
            btn.setOnMouseEntered(e -> {
                if (!action.equals(selectedQuickAction)) {
                    btn.setStyle(
                        "-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-font-size: 11px; " +
                        "-fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 20; " +
                        "-fx-border-color: #7dd3fc; -fx-border-width: 1; -fx-border-radius: 20; -fx-cursor: hand;"
                    );
                }
            });
            btn.setOnMouseExited(e -> {
                if (!action.equals(selectedQuickAction)) {
                    btn.setStyle(
                        "-fx-background-color: #f8fafc; -fx-text-fill: #334155; -fx-font-size: 11px; " +
                        "-fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 20; " +
                        "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-border-radius: 20; -fx-cursor: hand;"
                    );
                }
            });
            quickActionsRow.getChildren().add(btn);
        }
    }

    private void selectQuickAction(Button clicked, String action) {
        selectedQuickAction = action;
        errorLabel.setVisible(false);

        // Reset all buttons style
        for (var node : quickActionsRow.getChildren()) {
            if (node instanceof Button b) {
                b.setStyle(
                    "-fx-background-color: #f8fafc; -fx-text-fill: #334155; -fx-font-size: 11px; " +
                    "-fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 20; " +
                    "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-border-radius: 20; -fx-cursor: hand;"
                );
            }
        }

        // Highlight selected
        clicked.setStyle(
            "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-font-size: 11px; " +
            "-fx-font-weight: 700; -fx-padding: 6 14; -fx-background-radius: 20; " +
            "-fx-border-color: #0284c7; -fx-border-width: 1; -fx-border-radius: 20; -fx-cursor: hand;"
        );

        // Pre-fill message if empty
        if (messageArea.getText().trim().isEmpty()) {
            messageArea.setText(action + ". ");
            messageArea.positionCaret(messageArea.getText().length());
        }
    }

    @FXML
    public void handleAiSuggest() {
        if (demande == null) return;

        aiSuggestBtn.setDisable(true);
        aiSuggestBtn.setText("✨ Generation...");
        aiStatusLabel.setText("🤖 L'IA redige une reponse...");
        aiStatusLabel.setVisible(true);

        String bloodInfo = "";
        if ("Sang".equalsIgnoreCase(demande.getType())) {
            bloodInfo = " Le groupe sanguin demande est " + demande.getGroupeSanguin() + ".";
            if (donorBloodGroup.getValue() != null) {
                bloodInfo += " Le donneur a le groupe " + donorBloodGroup.getValue() + ".";
            }
        } else if ("Organe".equalsIgnoreCase(demande.getType())) {
            bloodInfo = " L'organe demande est: " + demande.getOrgane() + ".";
        }

        String prompt = "Tu es un assistant pour une plateforme de don medical. "
                + "Un donneur veut repondre a cette demande: \"" + demande.getTitre() + "\" - " + demande.getDescription()
                + ". Type de don: " + demande.getType() + ". Urgence: " + demande.getUrgence() + "." + bloodInfo
                + " Genere un message de reponse chaleureux et professionnel (3-4 phrases max) que le donneur peut envoyer. "
                + "Le message doit exprimer la volonte d'aider et la disponibilite. "
                + "Reponds UNIQUEMENT avec le message, sans guillemets ni prefixe.";

        new Thread(() -> {
            String result = aiService.analyzeText(prompt);
            Platform.runLater(() -> {
                messageArea.setText(result);
                aiSuggestBtn.setDisable(false);
                aiSuggestBtn.setText("🤖 Suggerer avec IA");
                aiStatusLabel.setText("✅ Message suggere par IA");
                new Thread(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> aiStatusLabel.setVisible(false));
                }).start();
            });
        }).start();
    }

    private void checkBloodCompatibility() {
        String donorGroup = donorBloodGroup.getValue();
        if (donorGroup == null || demande.getGroupeSanguin() == null) return;

        List<String> compatibles = BLOOD_MATRIX.getOrDefault(
                demande.getGroupeSanguin().toUpperCase(), new ArrayList<>());

        if (compatibles.contains(donorGroup)) {
            bloodMatchLabel.setText("✅ Compatible ! Votre groupe " + donorGroup + " peut aider.");
            bloodMatchLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #16a34a;");
            compatibilityBox.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 12; -fx-padding: 12; -fx-border-color: #bbf7d0; -fx-border-width: 1; -fx-border-radius: 12;");
            compatibilityLabel.setText("🎉 Votre groupe " + donorGroup + " est compatible avec " + demande.getGroupeSanguin());
            compatibilityLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #166534;");
        } else {
            bloodMatchLabel.setText("❌ Non compatible. Le receveur a besoin de: " + String.join(", ", compatibles));
            bloodMatchLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #dc2626;");
            compatibilityBox.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 12; -fx-padding: 12; -fx-border-color: #fecaca; -fx-border-width: 1; -fx-border-radius: 12;");
            compatibilityLabel.setText("⚠️ Votre groupe " + donorGroup + " n'est pas compatible avec " + demande.getGroupeSanguin());
            compatibilityLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #991b1b;");
        }
    }

    @FXML
    public void handleValider() {
        errorLabel.setVisible(false);

        // Must select a quick action OR write a message
        String msg = messageArea.getText().trim();
        if (selectedQuickAction == null && msg.isEmpty()) {
            errorLabel.setText("⚠️ Veuillez selectionner une action rapide ou ecrire un message.");
            errorLabel.setVisible(true);
            return;
        }

        // For blood donations, check if donor selected a blood group
        if ("Sang".equalsIgnoreCase(demande.getType()) && donorBloodGroup.getValue() == null) {
            errorLabel.setText("🩸 Veuillez selectionner votre groupe sanguin.");
            errorLabel.setVisible(true);
            return;
        }

        // Build the full message
        StringBuilder fullMessage = new StringBuilder();
        if (selectedQuickAction != null) {
            fullMessage.append("[").append(selectedQuickAction).append("] ");
        }
        if (!msg.isEmpty()) {
            fullMessage.append(msg);
        }
        if ("Sang".equalsIgnoreCase(demande.getType()) && donorBloodGroup.getValue() != null) {
            fullMessage.append(" | Groupe sanguin: ").append(donorBloodGroup.getValue());
        }
        if (shareContactCheck.isSelected() && SessionManager.getCurrentUser() != null) {
            var user = SessionManager.getCurrentUser();
            fullMessage.append(" | Contact: ").append(user.getEmail());
            if (user.getTelephone() != null && !user.getTelephone().isEmpty()) {
                fullMessage.append(", Tel: ").append(user.getTelephone());
            }
        }
        if (availableNowCheck.isSelected()) {
            fullMessage.append(" | Disponible immediatement");
        }

        // Get current user ID
        int currentDonorId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : 1;

        // Check for duplicate donation
        try {
            List<Don> existingDons = donService.afficherParDemande(demande.getId());
            for (Don existing : existingDons) {
                if (existing.getDonorId() == currentDonorId) {
                    errorLabel.setText("🛑 Vous avez deja repondu a cette demande.");
                    errorLabel.setVisible(true);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save the donation
        Don reaction = new Don(demande.getId(), currentDonorId, fullMessage.toString());

        try {
            donService.ajouter(reaction);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("✅ Reponse envoyee");
            alert.setHeaderText("🎉 Merci pour votre generosite !");
            String confirmMsg = "💚 Votre proposition a ete enregistree avec succes !";
            if ("Sang".equalsIgnoreCase(demande.getType()) && donorBloodGroup.getValue() != null) {
                List<String> compatibles = BLOOD_MATRIX.getOrDefault(
                        demande.getGroupeSanguin() != null ? demande.getGroupeSanguin().toUpperCase() : "", new ArrayList<>());
                if (compatibles.contains(donorBloodGroup.getValue())) {
                    confirmMsg += "\n\n🩸 Votre groupe sanguin " + donorBloodGroup.getValue() + " est compatible !";
                }
            }
            alert.setContentText(confirmMsg);
            alert.showAndWait();

            handleRetour();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("❌ Erreur: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    @FXML
    public void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/DemandeListView.fxml"));
            Parent root = loader.load();
            AideEtdonControllerClientController.getInstance().setView(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
