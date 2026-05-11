package org.example.servicesociaux.backoffice.controllers;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.backoffice.controller.AdminBaseController;
import org.example.servicesociaux.backoffice.entities.Hopital;
import org.example.servicesociaux.backoffice.services.HopitalService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * HopitalController Backoffice — implémente AdminAware.
 *
 * ✅ FIX : retourAccueil() utilise adminController.loadView() au lieu de
 *          recréer une Scene ou utiliser le singleton statique périmé.
 */
public class HopitalController implements AdminBaseController.AdminAware {

    // ── FXML Stats ──
    @FXML private Label statTotalHopitaux;
    @FXML private Label statUrgence;
    @FXML private Label statTotalRdv;
    @FXML private Label statEnAttente;

    // ── FXML Filtres ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterUrgence;
    @FXML private ComboBox<String> sortCombo;

    // ── FXML Formulaire ──
    @FXML private TextField       fieldNom;
    @FXML private TextField       fieldAdresse;
    @FXML private TextField       fieldTel;
    @FXML private TextField       fieldVille;
    @FXML private TextField       fieldSpecialites;
    @FXML private ComboBox<String> fieldType;
    @FXML private TextField       fieldCapacite;
    @FXML private TextField       fieldLatitude;
    @FXML private TextField       fieldLongitude;
    @FXML private CheckBox        checkUrgence;
    @FXML private Button          btnAjouter;
    @FXML private Button          btnModifier;
    @FXML private Button          btnSupprimer;
    @FXML private Button          btnVider;

    // ── FXML Tableau ──
    @FXML private TableView<Object[]>            tableView;
    @FXML private TableColumn<Object[], Integer> colId, colNbRdv, colRdvWait, colCapacite;
    @FXML private TableColumn<Object[], String>  colNom, colVille, colTel, colType, colSpec, colUrgence, colAdresse;

    @FXML private Label errorLabel;

    private final HopitalService service = new HopitalService();
    private ObservableList<Object[]> masterList   = FXCollections.observableArrayList();
    private FilteredList<Object[]>   filteredList;
    private int selectedId = -1;

    // ✅ Référence AdminBaseController injectée
    private AdminBaseController adminController;

    @Override
    public void setAdminController(AdminBaseController admin) {
        this.adminController = admin;
    }

    // ══ Init ══════════════════════════════════════════════════
    @FXML
    public void initialize() {
        // Colonnes
        colId      .setCellValueFactory(d -> new SimpleIntegerProperty((int) d.getValue()[0]).asObject());
        colNom     .setCellValueFactory(d -> new SimpleStringProperty(str(d.getValue()[1])));
        colVille   .setCellValueFactory(d -> new SimpleStringProperty(str(d.getValue()[5])));
        colTel     .setCellValueFactory(d -> new SimpleStringProperty(str(d.getValue()[3])));
        colSpec    .setCellValueFactory(d -> new SimpleStringProperty(str(d.getValue()[4])));
        colCapacite.setCellValueFactory(d -> new SimpleIntegerProperty((int) d.getValue()[8]).asObject());
        colUrgence .setCellValueFactory(d -> {
            boolean u = d.getValue()[9] != null && (boolean) d.getValue()[9];
            return new SimpleStringProperty(u ? "Oui" : "Non");
        });
        colNbRdv  .setCellValueFactory(d -> new SimpleIntegerProperty((int) d.getValue()[6]).asObject());
        colRdvWait.setCellValueFactory(d -> new SimpleIntegerProperty((int) d.getValue()[7]).asObject());
        colAdresse.setCellValueFactory(d -> new SimpleStringProperty(str(d.getValue()[2])));
        colType   .setCellValueFactory(d -> new SimpleStringProperty(str(d.getValue()[10])));

        // Style tableau
        tableView.setStyle(
                "-fx-background-color:white;-fx-border-color:#dde3f0;" +
                        "-fx-border-width:0.5;-fx-border-radius:10;-fx-background-radius:10;");

        // Types hôpital
        fieldType.getItems().addAll(
                "Public","Privé","Clinique","CHU","Polyclinique","Spécialisé");

        // Filtres
        filterUrgence.getItems().addAll("Tous","Urgence disponible","Sans urgence");
        filterUrgence.setValue("Tous");
        sortCombo.getItems().addAll(
                "Nom (A Z)","Nom (Z A)",
                "Nb RDV (croissant)","Nb RDV (decroissant)","En attente (decroissant)");
        sortCombo.setValue("Nom (A Z)");

        searchField  .textProperty().addListener((o,ov,nv) -> appliquerFiltres());
        filterUrgence.setOnAction(e -> appliquerFiltres());
        sortCombo    .setOnAction(e -> appliquerFiltres());

        // Clic tableau → remplir formulaire
        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null) remplirFormulaire(sel);
                });

        desactiverBtns();
        chargerTableau();
    }

    // ══ CRUD ══════════════════════════════════════════════════
    private void chargerTableau() {
        try {
            masterList.setAll(service.afficherAvecRendezVous());
            filteredList = new FilteredList<>(masterList, p -> true);
            tableView.setItems(filteredList);
            appliquerFiltres();
            mettreAJourStats();
        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    private void appliquerFiltres() {
        if (filteredList == null) return;
        String search = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        String fUrg   = filterUrgence.getValue() == null ? "Tous"
                : filterUrgence.getValue();

        filteredList.setPredicate(row -> {
            boolean ms = search.isEmpty()
                    || str(row[1]).toLowerCase().contains(search)
                    || str(row[2]).toLowerCase().contains(search)
                    || str(row[4]).toLowerCase().contains(search)
                    || str(row[5]).toLowerCase().contains(search)
                    || String.valueOf(row[0]).contains(search);
            boolean mu = fUrg.equals("Tous")
                    || (fUrg.equals("Urgence disponible") && row[9] != null && (boolean) row[9])
                    || (fUrg.equals("Sans urgence") && (row[9] == null || !(boolean) row[9]));
            return ms && mu;
        });

        ObservableList<Object[]> sorted = FXCollections.observableArrayList(filteredList);
        String sort = sortCombo.getValue();
        if (sort != null) {
            Comparator<Object[]> cmp = switch (sort) {
                case "Nom (Z A)"              -> Comparator.comparing((Object[] r) -> str(r[1])).reversed();
                case "Nb RDV (croissant)"     -> Comparator.comparingInt(r -> (int) r[6]);
                case "Nb RDV (decroissant)"   -> Comparator.comparingInt((Object[] r) -> (int) r[6]).reversed();
                case "En attente (decroissant)"-> Comparator.comparingInt((Object[] r) -> (int) r[7]).reversed();
                default                       -> Comparator.comparing(r -> str(r[1]));
            };
            sorted.sort(cmp);
        }
        tableView.setItems(sorted);
    }

    private void mettreAJourStats() {
        statTotalHopitaux.setText(String.valueOf(masterList.size()));
        statUrgence.setText(String.valueOf(
                masterList.stream().filter(r -> r[9] != null && (boolean) r[9]).count()));
        statTotalRdv.setText(String.valueOf(
                masterList.stream().mapToInt(r -> (int) r[6]).sum()));
        statEnAttente.setText(String.valueOf(
                masterList.stream().mapToInt(r -> (int) r[7]).sum()));
    }

    private void remplirFormulaire(Object[] row) {
        selectedId = (int) row[0];
        fieldNom        .setText(str(row[1]));
        fieldAdresse    .setText(str(row[2]));
        fieldTel        .setText(str(row[3]));
        fieldSpecialites.setText(str(row[4]));
        fieldVille      .setText(str(row[5]));
        fieldCapacite   .setText(String.valueOf(row[8]));
        checkUrgence    .setSelected(row[9] != null && (boolean) row[9]);
        activerBtns();
    }

    @FXML
    public void ajouter() {
        try {
            Hopital h = buildFromForm();
            if (h == null) return;
            service.ajouter(h);
            showSuccess("Hopital ajoute avec succes !");
            viderFormulaire();
            chargerTableau();
        } catch (SQLException e) {
            showError("Erreur ajout : " + e.getMessage());
        }
    }

    @FXML
    public void modifier() {
        if (selectedId < 0) { showError("Selectionnez un hopital."); return; }
        try {
            Hopital h = buildFromForm();
            if (h == null) return;
            h.setId(selectedId);
            service.modifier(h);
            showSuccess("Hopital modifie avec succes !");
            viderFormulaire();
            chargerTableau();
        } catch (SQLException e) {
            showError("Erreur modification : " + e.getMessage());
        }
    }

    @FXML
    public void supprimer() {
        if (selectedId < 0) { showError("Selectionnez un hopital."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'hopital #" + selectedId + " ?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.supprimer(selectedId);
                    showSuccess("Hopital supprime.");
                    viderFormulaire();
                    chargerTableau();
                } catch (Exception e) {
                    showError("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void viderFormulaire() {
        selectedId = -1;
        fieldNom.clear(); fieldAdresse.clear(); fieldTel.clear();
        fieldVille.clear(); fieldSpecialites.clear();
        fieldCapacite.clear(); fieldLatitude.clear(); fieldLongitude.clear();
        fieldType.setValue(null);
        checkUrgence.setSelected(false);
        tableView.getSelectionModel().clearSelection();
        desactiverBtns();
        errorLabel.setText("");
    }

    private Hopital buildFromForm() {
        String nom = fieldNom.getText().trim();
        if (nom.isEmpty()) { showError("Le nom est obligatoire."); return null; }
        Hopital h = new Hopital();
        h.setNom(nom);
        h.setAdresse(fieldAdresse.getText().trim());
        h.setTelephone(fieldTel.getText().trim());
        h.setVille(fieldVille.getText().trim());
        h.setSpecialites(fieldSpecialites.getText().trim());
        h.setType(fieldType.getValue());
        h.setServiceUrgenceDispo(checkUrgence.isSelected());
        try { h.setCapacite(Integer.parseInt(fieldCapacite.getText().trim())); }
        catch (NumberFormatException e) { h.setCapacite(0); }
        try { h.setLatitude(Double.parseDouble(fieldLatitude.getText().trim())); }
        catch (NumberFormatException e) { h.setLatitude(0); }
        try { h.setLongitude(Double.parseDouble(fieldLongitude.getText().trim())); }
        catch (NumberFormatException e) { h.setLongitude(0); }
        return h;
    }

    @FXML
    public void ouvrirCarte() {
        // TODO : ouvrir sélecteur de coordonnées sur carte
        showSuccess("Fonctionnalite carte a implementer.");
    }

    @FXML
    public void exporterCSV() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("hopitaux_admin.csv");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File f = fc.showSaveDialog((Stage) tableView.getScene().getWindow());
        if (f == null) return;
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("ID,Nom,Ville,Tel,Specialites,Urgence,RDV,Attente\n");
            for (Object[] row : tableView.getItems()) {
                boolean urg = row[9] != null && (boolean) row[9];
                fw.write(String.format("%s,\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,%s\n",
                        row[0], str(row[1]), str(row[5]), str(row[3]),
                        str(row[4]), urg ? "Oui" : "Non", row[6], row[7]));
            }
            showSuccess("Export CSV : " + f.getName());
        } catch (IOException e) {
            showError("Erreur export : " + e.getMessage());
        }
    }

    // ✅ FIX PRINCIPAL : retourAccueil utilise adminController injecté
    @FXML
    public void retourAccueil() {
        if (adminController != null) {
            // ✅ Recharge MainMenu dans adminContentArea — référence toujours valide
            adminController.loadView(
                    "/servicesociaux/backoffice/MainMenu.fxml", null);
        } else {
            System.err.println("❌ adminController non injecte dans HopitalController");
        }
    }

    private void activerBtns() {
        btnModifier.setDisable(false);
        btnSupprimer.setDisable(false);
    }
    private void desactiverBtns() {
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#c62828;-fx-font-size:11;");
        errorLabel.setText(msg);
        PauseTransition p = new PauseTransition(Duration.seconds(5));
        p.setOnFinished(e -> errorLabel.setText(""));
        p.play();
    }
    private void showSuccess(String msg) {
        errorLabel.setStyle("-fx-text-fill:#2e7d32;-fx-font-size:11;");
        errorLabel.setText(msg);
        PauseTransition p = new PauseTransition(Duration.seconds(4));
        p.setOnFinished(e -> errorLabel.setText(""));
        p.play();
    }
    private String str(Object o) { return o != null ? o.toString() : ""; }
}