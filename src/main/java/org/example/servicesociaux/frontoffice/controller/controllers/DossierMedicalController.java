package org.example.servicesociaux.frontoffice.controller.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.user.model.DossierMedical;
import org.example.user.service.ServiceDossierMedical;
import org.example.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DossierMedicalController {

    // ── Liste ──
    @FXML private VBox   listPanel;
    @FXML private VBox   dossiersListBox;

    // ── Formulaire ──
    @FXML private VBox   formPanel;
    @FXML private TextArea antecedentsArea, maladiesArea, allergiesArea;
    @FXML private TextArea traitementsArea, diagnosticsArea, notesArea, objectifArea;
    @FXML private ComboBox<String> activiteCombo;
    @FXML private Label  formTitleLabel;

    // ── Detail dossier ──
    @FXML private VBox   detailPanel;
    @FXML private Label  detailTitleLabel;
    @FXML private Button btnGroq;
    @FXML private Button btnHF;

    // ── Statut / erreur ──
    @FXML private Label  statusLabel;
    @FXML private Label  errorLabel;

    private final ServiceDossierMedical service = new ServiceDossierMedical();
    private DossierMedical dossierEnCours;
    private DossierMedical dossierSelectionne;  // dossier affiché à droite
    private int userId = -1;

    // ══════════════════════════════════════════════
    @FXML
    public void initialize() {
        activiteCombo.getItems().addAll(
                "Sedentaire", "Leger", "Modere", "Actif", "Tres actif");
        activiteCombo.setValue("Sedentaire");

        formPanel.setVisible(false);
        formPanel.setManaged(false);

        try {
            var user = SessionManager.getCurrentUser();
            if (user != null) userId = user.getId();
        } catch (Exception ignored) {}

        chargerListe();
    }

    // ══ LISTE ═════════════════════════════════════
    private void chargerListe() {
        dossiersListBox.getChildren().clear();
        try {
            List<DossierMedical> liste = userId > 0
                    ? service.findAllByUserId(userId)
                    : service.findAll();

            statusLabel.setText(liste.size() + " dossier(s)");

            if (liste.isEmpty()) {
                Label empty = new Label("Aucun dossier medical. Cliquez sur + Nouveau.");
                empty.setStyle("-fx-font-size:12;-fx-text-fill:#aaa;-fx-padding:30;");
                dossiersListBox.getChildren().add(empty);
                return;
            }

            for (DossierMedical d : liste) {
                dossiersListBox.getChildren().add(creerCarte(d));
            }

            // Selectionner le premier par defaut
            afficherDetail(liste.get(0));

        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    // ══ CARTE DOSSIER ══════════════════════════════
    private VBox creerCarte(DossierMedical d) {
        VBox card = new VBox(6);
        card.setStyle(styleCard(false));
        card.setCursor(javafx.scene.Cursor.HAND);

        // Ligne 1 : ID + date
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label idLbl = new Label("Dossier #" + d.getId());
        idLbl.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:#1e293b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label dateLbl = new Label(d.getDateCreationFormatee());
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#94a3b8;");
        top.getChildren().addAll(idLbl, sp, dateLbl);

        // Ligne 2 : maladies (apercu)
        String maladie = d.getMaladiesChroniques() != null ? d.getMaladiesChroniques() : "Aucune maladie renseignee";
        Label malLbl = new Label(maladie.length() > 55 ? maladie.substring(0, 55) + "..." : maladie);
        malLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");
        malLbl.setWrapText(true);

        // Ligne 3 : badge activite + boutons
        HBox bot = new HBox(8);
        bot.setAlignment(Pos.CENTER_LEFT);
        Label actBadge = new Label(nvl(d.getNiveauActivite(), "—"));
        actBadge.getStyleClass().add("badge-activite");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

        Button edit = smallBtn("Modifier", "#f1f5f9", "#1e293b");
        edit.setOnAction(e -> ouvrirFormulaire(d));
        edit.setStyle(edit.getStyle() + "-fx-font-weight:bold;");

        Button del = smallBtn("Supprimer", "#fef2f2", "#dc3545");
        del.setOnAction(e -> supprimerDossier(d));
        del.setStyle(del.getStyle() + "-fx-font-weight:bold;");

        bot.getChildren().addAll(actBadge, sp2, edit, del);
        card.getChildren().addAll(top, malLbl, bot);

        // Clic → afficher detail à droite
        card.setOnMouseClicked(e -> afficherDetail(d));
        card.setOnMouseEntered(e -> card.setStyle(styleCard(true)));
        card.setOnMouseExited(e  -> card.setStyle(styleCard(false)));

        return card;
    }

    private String styleCard(boolean hover) {
        return "-fx-background-color:white;" +
                "-fx-border-color:" + (hover ? "#dc3545" : "#e2e8f0") + ";" +
                "-fx-border-radius:12;-fx-background-radius:12;" +
                "-fx-padding:15;-fx-effect:dropshadow(three-pass-box," +
                (hover ? "rgba(220,53,69,0.15)" : "rgba(0,0,0,0.02)") + ",10,0,0,4);";
    }

    private Button smallBtn(String txt, String bg, String fg) {
        Button b = new Button(txt);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-background-radius:6;-fx-font-size:10;" +
                "-fx-padding:4 10;-fx-cursor:hand;-fx-border-color:transparent;");
        return b;
    }

    // ══ DETAIL DOSSIER (colonne droite) ═══════════
    private void afficherDetail(DossierMedical d) {
        dossierSelectionne = d;
        detailTitleLabel.setText("Dossier #" + d.getId());
        btnGroq.setVisible(true); btnGroq.setManaged(true);
        btnHF.setVisible(true);   btnHF.setManaged(true);

        detailPanel.getChildren().clear();

        // Carte score/dates
        HBox metaBox = new HBox(16);
        metaBox.getStyleClass().add("detail-header-card");

        VBox dateCreBox = metaVBox("Date de création", d.getDateCreationFormatee(), "#dc3545");
        VBox dateMajBox = metaVBox("Dernière modification", d.getDerniereMiseAJourFormatee(), "#64748b");
        VBox actBox     = metaVBox("Niveau d'activité", nvl(d.getNiveauActivite(), "Non renseigné"), "#10b981");

        metaBox.getChildren().addAll(dateCreBox, dateMajBox, actBox);
        detailPanel.getChildren().add(metaBox);

        // Sections medicales
        addSection(detailPanel, "Maladies chroniques",    d.getMaladiesChroniques(),    "#dc3545", "#fef2f2");
        addSection(detailPanel, "Antécédents médicaux",   d.getAntecedentsMedicaux(),   "#64748b", "#f8f9fb");
        addSection(detailPanel, "Allergies",               d.getAllergies(),              "#ea580c", "#fff7ed");
        addSection(detailPanel, "Traitements en cours",   d.getTraitementsEnCours(),    "#3b82f6", "#eff6ff");
        addSection(detailPanel, "Diagnostics",             d.getDiagnostics(),           "#10b981", "#f0fdf4");
        addSection(detailPanel, "Notes du médecin",        d.getNotesMedecin(),          "#475569", "#f1f5f9");
        addSection(detailPanel, "Objectif santé",          d.getObjectifSante(),         "#dc3545", "#fef2f2");

        // Boutons IA en bas du detail
        HBox iaBtns = new HBox(10);
        iaBtns.getStyleClass().add("detail-header-card");
        iaBtns.setAlignment(Pos.CENTER);

        Button g = new Button("Analyser avec Groq LLaMA");
        g.getStyleClass().add("btn-ia-groq");
        g.setOnAction(e -> ouvrirAnalyseGroq());

        Button h = new Button("Analyser avec HuggingFace");
        h.getStyleClass().add("btn-ia-hf");
        h.setOnAction(e -> ouvrirAnalyseHF());

        iaBtns.getChildren().addAll(g, h);
        detailPanel.getChildren().add(iaBtns);
    }

    private void addSection(VBox parent, String titre, String contenu,
                            String color, String bgColor) {
        String texte = contenu != null && !contenu.isBlank()
                ? contenu : "Non renseigne";

        VBox box = new VBox(8);
        box.getStyleClass().add("medical-section");
        box.setStyle("-fx-border-color:" + color + "; -fx-background-color:" + bgColor + ";");

        Label titre_lbl = new Label(titre.toUpperCase());
        titre_lbl.getStyleClass().add("section-label");
        titre_lbl.setStyle("-fx-text-fill:" + color + ";");

        Label contenu_lbl = new Label(texte);
        contenu_lbl.setWrapText(true);
        contenu_lbl.getStyleClass().add("section-content");

        box.getChildren().addAll(titre_lbl, contenu_lbl);
        parent.getChildren().add(box);
    }

    private VBox metaVBox(String label, String valeur, String color) {
        VBox v = new VBox(3);
        HBox.setHgrow(v, Priority.ALWAYS);
        Label l = new Label(label);
        l.setStyle("-fx-font-size:10;-fx-text-fill:#aaa;");
        Label val = new Label(valeur != null ? valeur : "—");
        val.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        v.getChildren().addAll(l, val);
        return v;
    }

    // ══ OUVRIR PAGE ANALYSE ════════════════════════
    @FXML
    public void ouvrirAnalyseGroq() {
        if (dossierSelectionne == null) { showError("Selectionnez un dossier."); return; }
        ouvrirAnalyse("GROQ");
    }

    @FXML
    public void ouvrirAnalyseHF() {
        if (dossierSelectionne == null) { showError("Selectionnez un dossier."); return; }
        ouvrirAnalyse("HF");
    }

    private void ouvrirAnalyse(String mode) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/servicesociaux/frontoffice/analyseAI.fxml")
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 1050, 680));
            stage.setTitle("Analyse IA — Dossier #" + dossierSelectionne.getId());

            AnalyseIAController ctrl = loader.getController();
            ctrl.setDossier(dossierSelectionne, mode);

            stage.show();
        } catch (IOException e) {
            showError("Erreur ouverture analyse : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══ FORMULAIRE ════════════════════════════════
    @FXML
    public void afficherFormulaireNouveauDossier() {
        dossierEnCours = null;
        vider();
        formTitleLabel.setText("Nouveau dossier medical");
        formPanel.setVisible(true);  formPanel.setManaged(true);
        listPanel.setVisible(false); listPanel.setManaged(false);
    }

    private void ouvrirFormulaire(DossierMedical d) {
        dossierEnCours = d;
        antecedentsArea.setText(nvl(d.getAntecedentsMedicaux(), ""));
        maladiesArea   .setText(nvl(d.getMaladiesChroniques(),  ""));
        allergiesArea  .setText(nvl(d.getAllergies(),            ""));
        traitementsArea.setText(nvl(d.getTraitementsEnCours(),  ""));
        diagnosticsArea.setText(nvl(d.getDiagnostics(),         ""));
        notesArea      .setText(nvl(d.getNotesMedecin(),        ""));
        objectifArea   .setText(nvl(d.getObjectifSante(),       ""));
        String act = d.getNiveauActivite();
        activiteCombo.setValue(act != null && !act.isBlank() ? act : "Sedentaire");
        formTitleLabel.setText("Modifier dossier #" + d.getId());
        formPanel.setVisible(true);  formPanel.setManaged(true);
        listPanel.setVisible(false); listPanel.setManaged(false);
    }

    @FXML
    public void fermerFormulaire() {
        formPanel.setVisible(false); formPanel.setManaged(false);
        listPanel.setVisible(true);  listPanel.setManaged(true);
        chargerListe();
    }

    @FXML
    public void sauvegarder() {
        try {
            if (dossierEnCours == null) {
                dossierEnCours = new DossierMedical();
                dossierEnCours.setUserId(userId > 0 ? userId : 0);
            }
            dossierEnCours.setAntecedentsMedicaux(antecedentsArea.getText().trim());
            dossierEnCours.setMaladiesChroniques (maladiesArea   .getText().trim());
            dossierEnCours.setAllergies          (allergiesArea  .getText().trim());
            dossierEnCours.setTraitementsEnCours (traitementsArea.getText().trim());
            dossierEnCours.setDiagnostics        (diagnosticsArea.getText().trim());
            dossierEnCours.setNotesMedecin       (notesArea      .getText().trim());
            dossierEnCours.setObjectifSante      (objectifArea   .getText().trim());
            dossierEnCours.setNiveauActivite     (activiteCombo.getValue());

            if (dossierEnCours.getId() == 0) service.add(dossierEnCours);
            else                              service.update(dossierEnCours);

            showSuccess("Dossier sauvegarde !");
            fermerFormulaire();
        } catch (SQLException e) {
            showError("Erreur SQL : " + e.getMessage());
        }
    }

    private void supprimerDossier(DossierMedical d) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le dossier #" + d.getId() + " ?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(d.getId());
                    showSuccess("Dossier supprime.");
                    detailPanel.getChildren().clear();
                    btnGroq.setVisible(false); btnGroq.setManaged(false);
                    btnHF.setVisible(false);   btnHF.setManaged(false);
                    detailTitleLabel.setText("Selectionnez un dossier");
                    dossierSelectionne = null;
                    chargerListe();
                } catch (Exception e) {
                    showError("Erreur : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void vider() {
        antecedentsArea.clear(); maladiesArea.clear();
        allergiesArea.clear();   traitementsArea.clear();
        diagnosticsArea.clear(); notesArea.clear();
        objectifArea.clear();    activiteCombo.setValue("Sedentaire");
    }

    @FXML
    public void retourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/servicesociaux/frontoffice/mainMenu.fxml"));
            Stage stage = (Stage) detailPanel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 900, 650));
            stage.setTitle("MediCare — Accueil");
        } catch (Exception e) {
            showError("Navigation : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#c62828;");
        errorLabel.setText(msg);
        PauseTransition p = new PauseTransition(Duration.seconds(5));
        p.setOnFinished(e -> errorLabel.setText(""));
        p.play();
    }

    private void showSuccess(String msg) {
        errorLabel.setStyle("-fx-text-fill:#2e7d32;");
        errorLabel.setText(msg);
        PauseTransition p = new PauseTransition(Duration.seconds(4));
        p.setOnFinished(e -> errorLabel.setText(""));
        p.play();
    }

    private String nvl(String s, String def) { return (s != null && !s.isBlank()) ? s : def; }
}