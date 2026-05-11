package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.aideEtdon.model.ContactUrgence;
import org.example.aideEtdon.service.ContactUrgenceService;

import java.io.IOException;
import java.util.List;

public class ContactFormController {

    // View Components
    @FXML private VBox contactsContainer;
    
    // Form Components
    @FXML private Label formTitleLabel;
    @FXML private TextField fldNom;
    @FXML private TextField fldEmail;
    @FXML private TextField fldTel;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancelEdit;

    private ContactUrgenceService service = new ContactUrgenceService();
    private ContactUrgence currentEditingContact = null;

    @FXML
    public void initialize() {
        loadContacts();
    }

    private int contactCardIndex = 0;

    private void loadContacts() {
        contactsContainer.getChildren().clear();
        contactCardIndex = 0;
        List<ContactUrgence> list = service.afficherToutes();

        if (list.isEmpty()) {
            Label empty = new Label("💭 Aucun contact enregistré.\n➕ Veuillez en ajouter un à l'aide du formulaire.");
            empty.getStyleClass().add("empty-state-label");
            contactsContainer.getChildren().add(empty);
            return;
        }

        for (ContactUrgence contact : list) {
            HBox card = new HBox(15);
            card.getStyleClass().add("contact-item-card");
            card.setOpacity(0);
            card.setTranslateY(15);

            // Staggered entrance animation
            FadeTransition ft = new FadeTransition(Duration.millis(350), card);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(contactCardIndex * 70));

            TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
            tt.setToY(0);
            tt.setDelay(Duration.millis(contactCardIndex * 70));

            ft.play();
            tt.play();
            contactCardIndex++;

            VBox infoBox = new VBox(6);
            Label nameLbl = new Label(contact.getNom());
            nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");

            Label emailLbl = new Label("📧 " + contact.getEmail());
            emailLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-family: 'Segoe UI', sans-serif;");

            Label telLbl = new Label(contact.getTelephone() == null || contact.getTelephone().isEmpty() ? "📱 Non renseigné" : "📱 " + contact.getTelephone());
            telLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI', sans-serif;");

            infoBox.getChildren().addAll(nameLbl, emailLbl, telLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnModif = new Button("✏️ Modifier");
            btnModif.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-text-fill: white; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 10px; -fx-padding: 8 16; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.20), 8, 0, 0, 2);");
            btnModif.setOnMouseEntered(e -> btnModif.setStyle("-fx-background-color: linear-gradient(to right, #d97706, #b45309); -fx-text-fill: white; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 10px; -fx-padding: 8 16; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(217,119,6,0.25), 10, 0, 0, 3); -fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
            btnModif.setOnMouseExited(e -> btnModif.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); -fx-text-fill: white; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 10px; -fx-padding: 8 16; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.20), 8, 0, 0, 2); -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));
            btnModif.setOnAction(e -> triggerEdit(contact));

            Button btnDel = new Button("🗑 Supprimer");
            btnDel.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 10px; -fx-padding: 8 16; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.20), 8, 0, 0, 2);");
            btnDel.setOnMouseEntered(e -> btnDel.setStyle("-fx-background-color: linear-gradient(to right, #dc2626, #b91c1c); -fx-text-fill: white; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 10px; -fx-padding: 8 16; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(220,38,38,0.30), 10, 0, 0, 3); -fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
            btnDel.setOnMouseExited(e -> btnDel.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 10px; -fx-padding: 8 16; -fx-font-family: 'Segoe UI', sans-serif; -fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.20), 8, 0, 0, 2); -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));
            btnDel.setOnAction(e -> {
                service.supprimer(contact.getId());
                if (currentEditingContact != null && currentEditingContact.getId() == contact.getId()) {
                    cancelEdit();
                }
                loadContacts();
            });

            card.getChildren().addAll(infoBox, spacer, btnModif, btnDel);
            contactsContainer.getChildren().add(card);
        }
    }

    private void shakeNode(Node node) {
        TranslateTransition shake1 = new TranslateTransition(Duration.millis(60), node);
        shake1.setByX(6);
        TranslateTransition shake2 = new TranslateTransition(Duration.millis(60), node);
        shake2.setByX(-12);
        TranslateTransition shake3 = new TranslateTransition(Duration.millis(60), node);
        shake3.setByX(12);
        TranslateTransition shake4 = new TranslateTransition(Duration.millis(60), node);
        shake4.setByX(-6);
        shake1.setOnFinished(e -> shake2.play());
        shake2.setOnFinished(e -> shake3.play());
        shake3.setOnFinished(e -> shake4.play());
        shake1.play();
    }

    private void triggerEdit(ContactUrgence contact) {
        currentEditingContact = contact;
        formTitleLabel.setText("✏️ Modifier le contact");
        btnSave.setText("💾 METTRE A JOUR");
        btnCancelEdit.setVisible(true);

        fldNom.setText(contact.getNom());
        fldEmail.setText(contact.getEmail());
        fldTel.setText(contact.getTelephone() != null ? contact.getTelephone() : "");
    }

    @FXML
    private void cancelEdit() {
        currentEditingContact = null;
        formTitleLabel.setText("➕ Ajouter un contact");
        btnSave.setText("💾 ENREGISTRER");
        btnCancelEdit.setVisible(false);

        fldNom.clear();
        fldEmail.clear();
        fldTel.clear();
    }

    @FXML
    private void handleSave() {
        lblError.setVisible(false);
        String nom = fldNom.getText().trim();
        String email = fldEmail.getText().trim();
        String tel = fldTel.getText().trim();

        if (nom.isEmpty() || email.isEmpty()) {
            lblError.setText("⚠️ Veuillez remplir le Nom et l'Email.");
            lblError.setVisible(true);
            shakeNode(lblError);
            if (nom.isEmpty()) shakeNode(fldNom);
            if (email.isEmpty()) shakeNode(fldEmail);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            lblError.setText("❌ Format d'email invalide.");
            lblError.setVisible(true);
            shakeNode(lblError);
            shakeNode(fldEmail);
            return;
        }

        if (!tel.isEmpty() && !tel.matches("\\d{8,15}")) {
            lblError.setText("📱 Le téléphone doit contenir entre 8 et 15 chiffres.");
            lblError.setVisible(true);
            shakeNode(lblError);
            shakeNode(fldTel);
            return;
        }

        if (currentEditingContact == null) {
            // Add new
            service.enregistrer(new ContactUrgence(nom, email, tel));
        } else {
            // Update existing
            currentEditingContact.setNom(nom);
            currentEditingContact.setEmail(email);
            currentEditingContact.setTelephone(tel);
            service.modifier(currentEditingContact);
            cancelEdit();
        }

        loadContacts();
        fldNom.clear();
        fldEmail.clear();
        fldTel.clear();

        // Success feedback animation on save button
        FadeTransition glow = new FadeTransition(Duration.millis(300), btnSave);
        glow.setFromValue(1.0);
        glow.setToValue(0.7);
        glow.setAutoReverse(true);
        glow.setCycleCount(2);
        glow.play();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/AideHomeView.fxml"));
            Node homeView = loader.load();
            AideEtdonControllerClientController.getInstance().setView(homeView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
