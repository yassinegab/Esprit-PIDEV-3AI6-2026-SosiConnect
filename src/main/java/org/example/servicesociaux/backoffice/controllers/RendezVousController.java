package org.example.servicesociaux.backoffice.controllers;

import javafx.animation.PauseTransition;
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
import org.example.servicesociaux.backoffice.entities.RendezVous;
import org.example.servicesociaux.backoffice.services.RendezVousService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * RendezVousController Backoffice — implémente AdminAware.
 *
 * ✅ FIX : retourAccueil() utilise adminController.loadView() injecté.
 */
public class RendezVousController implements AdminBaseController.AdminAware {

    // ── Stats ──
    @FXML private Label statTotal;
    @FXML private Label statEnAttente;
    @FXML private Label statConfirme;
    @FXML private Label statTermine;
    @FXML private Label statAnnule;

    // ── Filtres ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterHopital;

    // ── Tableau ──
    @FXML private TableView<RendezVous>            tableView;
    @FXML private TableColumn<RendezVous, Integer> colId;
    @FXML private TableColumn<RendezVous, Integer> colPatient;
    @FXML private TableColumn<RendezVous, String>  colHopital;
    @FXML private TableColumn<RendezVous, String>  colType;
    @FXML private TableColumn<RendezVous, String>  colStatut;
    @FXML private TableColumn<RendezVous, Date>    colDate;
    @FXML private TableColumn<RendezVous, String>  colNotes;

    // ── Formulaire modification ──
    @FXML private Label            labelSelectedId;
    @FXML private ComboBox<String> fieldStatut;
    @FXML private TextArea         fieldNotes;
    @FXML private Button           btnModifier;
    @FXML private Button           btnSupprimer;
    @FXML private Button           btnVider;

    @FXML private Label errorLabel;

    private final RendezVousService    service     = new RendezVousService();
    private final Map<String, Integer> hopitauxMap = new LinkedHashMap<>();
    private ObservableList<RendezVous> masterList  = FXCollections.observableArrayList();
    private FilteredList<RendezVous>   filteredList;
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
        colId     .setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        colHopital.setCellValueFactory(new PropertyValueFactory<>("hopitalNom"));
        colType   .setCellValueFactory(new PropertyValueFactory<>("typeConsultation"));
        colStatut .setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate   .setCellValueFactory(new PropertyValueFactory<>("dateRendezVous"));
        colNotes  .setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Cellule statut colorée
        colId     .setCellFactory(col -> createCell());
        colPatient.setCellFactory(col -> createCell());
        colHopital.setCellFactory(col -> createCell());
        colType   .setCellFactory(col -> createCell());
        colDate   .setCellFactory(col -> createCell());
        colNotes  .setCellFactory(col -> createCell());
        colStatut .setCellFactory(col -> createStatutCell());

        tableView.setStyle(
                "-fx-background-color:white;-fx-border-color:#dde3f0;" +
                        "-fx-border-width:0.5;-fx-border-radius:10;-fx-background-radius:10;");

        // Statuts formulaire
        fieldStatut.getItems().addAll(
                "En attente","Confirme","Termine","Annule");

        // Filtres
        filterStatut.getItems().add("Tous");
        filterStatut.getItems().addAll(
                "En attente","Confirme","Termine","Annule");
        filterStatut.setValue("Tous");

        filterType.getItems().add("Tous");
        filterType.getItems().addAll(
                "Presentiel","Teleconsultation","Urgence");
        filterType.setValue("Tous");

        try {
            Map<Integer, String> hopitaux = service.getHopitaux();
            filterHopital.getItems().add("Tous");
            hopitaux.forEach((id, nom) -> {
                hopitauxMap.put(nom, id);
                filterHopital.getItems().add(nom);
            });
        } catch (SQLException e) {
            showError("Impossible de charger les hopitaux");
        }
        filterHopital.setValue("Tous");

        // Listeners filtres
        searchField  .textProperty().addListener((o,ov,nv) -> appliquerFiltres());
        filterStatut .setOnAction(e -> appliquerFiltres());
        filterType   .setOnAction(e -> appliquerFiltres());
        filterHopital.setOnAction(e -> appliquerFiltres());

        // Clic tableau → formulaire
        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null) remplirFormulaire(sel);
                });

        desactiverFormulaire();
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);
        chargerTableau();
    }

    // ══ Chargement ════════════════════════════════════════════
    private void chargerTableau() {
        try {
            masterList.setAll(service.afficherTous());
            appliquerFiltres();
            mettreAJourStats();
        } catch (SQLException e) {
            showError("Erreur chargement : " + e.getMessage());
        }
    }

    // ══ Filtrage ══════════════════════════════════════════════
    private void appliquerFiltres() {
        String search  = searchField .getText() == null ? "" : searchField .getText().toLowerCase().trim();
        String fStatut = filterStatut.getValue() == null ? "Tous" : filterStatut.getValue();
        String fType   = filterType  .getValue() == null ? "Tous" : filterType  .getValue();
        String fHop    = filterHopital.getValue()== null ? "Tous" : filterHopital.getValue();

        filteredList.setPredicate(rdv -> {
            boolean ms = search.isEmpty()
                    || String.valueOf(rdv.getId())       .contains(search)
                    || String.valueOf(rdv.getPatientId()).contains(search)
                    || str(rdv.getHopitalNom())      .toLowerCase().contains(search)
                    || str(rdv.getTypeConsultation()).toLowerCase().contains(search)
                    || str(rdv.getStatut())          .toLowerCase().contains(search)
                    || str(rdv.getNotes())           .toLowerCase().contains(search)
                    || (rdv.getDateRendezVous() != null &&
                    rdv.getDateRendezVous().toString().contains(search));

            boolean mSt = fStatut.equals("Tous") || fStatut.equals(rdv.getStatut());
            boolean mTy = fType  .equals("Tous") || fType  .equals(rdv.getTypeConsultation());
            boolean mHp = fHop   .equals("Tous") || fHop   .equals(rdv.getHopitalNom());
            return ms && mSt && mTy && mHp;
        });
    }

    private void mettreAJourStats() {
        statTotal    .setText(String.valueOf(masterList.size()));
        statEnAttente.setText(String.valueOf(masterList.stream()
                .filter(r -> "En attente".equals(r.getStatut())).count()));
        statConfirme .setText(String.valueOf(masterList.stream()
                .filter(r -> "Confirme"  .equals(r.getStatut())).count()));
        statTermine  .setText(String.valueOf(masterList.stream()
                .filter(r -> "Termine"   .equals(r.getStatut())).count()));
        statAnnule   .setText(String.valueOf(masterList.stream()
                .filter(r -> "Annule"    .equals(r.getStatut())).count()));
    }

    // ══ Formulaire ════════════════════════════════════════════
    private void remplirFormulaire(RendezVous rdv) {
        selectedId = rdv.getId();
        labelSelectedId.setText(
                "RDV selectionne : #" + selectedId + " — " + str(rdv.getHopitalNom()));
        labelSelectedId.setStyle(
                "-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#3a7bd5;" +
                        "-fx-padding:4 8;-fx-background-color:#eef3fb;-fx-background-radius:6;");
        fieldStatut.setValue(rdv.getStatut());
        fieldNotes .setText(rdv.getNotes() != null ? rdv.getNotes() : "");
        activerFormulaire();
    }

    // ══ CRUD ══════════════════════════════════════════════════
    @FXML
    public void modifier() {
        if (selectedId == -1) {
            showError("Selectionnez un rendez-vous dans le tableau."); return; }
        if (fieldStatut.getValue() == null) {
            showError("Le statut est obligatoire."); return; }
        try {
            RendezVous existing = masterList.stream()
                    .filter(r -> r.getId() == selectedId).findFirst().orElse(null);
            if (existing == null) { showError("RDV introuvable."); return; }

            existing.setStatut(fieldStatut.getValue());
            existing.setNotes(fieldNotes.getText().trim());
            service.modifier(existing);

            for (int i = 0; i < masterList.size(); i++) {
                if (masterList.get(i).getId() == selectedId) {
                    masterList.set(i, existing); break;
                }
            }
            tableView.refresh();
            mettreAJourStats();
            showSuccess("Rendez-vous #" + selectedId + " modifie.");
            viderFormulaire();
        } catch (Exception e) {
            showError("Erreur modification : " + e.getMessage());
        }
    }

    @FXML
    public void supprimer() {
        if (selectedId == -1) {
            showError("Selectionnez un rendez-vous a supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le rendez-vous #" + selectedId + " ?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        try {
            service.supprimer(selectedId);
            masterList.removeIf(r -> r.getId() == selectedId);
            tableView.refresh();
            mettreAJourStats();
            showSuccess("Rendez-vous #" + selectedId + " supprime.");
            viderFormulaire();
        } catch (SQLException e) {
            showError("Erreur suppression : " + e.getMessage());
        }
    }

    @FXML
    public void viderFormulaire() {
        selectedId = -1;
        labelSelectedId.setText("Aucun rendez-vous selectionne");
        labelSelectedId.setStyle("-fx-font-size:10;-fx-text-fill:#aaa;-fx-padding:4 8;");
        fieldStatut.setValue(null);
        fieldNotes .clear();
        tableView.getSelectionModel().clearSelection();
        desactiverFormulaire();
        errorLabel.setText("");
    }

    @FXML
    public void exporterCSV() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("rendez_vous_admin.csv");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File f = fc.showSaveDialog((Stage) tableView.getScene().getWindow());
        if (f == null) return;
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("ID,Patient ID,Hopital,Type,Statut,Date,Notes\n");
            for (RendezVous rdv : tableView.getItems()) {
                fw.write(String.format("%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        rdv.getId(), rdv.getPatientId(),
                        esc(rdv.getHopitalNom()), esc(rdv.getTypeConsultation()),
                        esc(rdv.getStatut()),
                        rdv.getDateRendezVous() != null
                                ? rdv.getDateRendezVous().toString() : "",
                        esc(rdv.getNotes())));
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
            System.err.println(
                    "❌ adminController non injecte dans RendezVousController");
        }
    }

    // ══ Cellules ══════════════════════════════════════════════
    private <T> TableCell<RendezVous, T> createCell() {
        return new TableCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle(getTableRow() != null && getTableRow().isSelected()
                        ? "-fx-text-fill:#1a2744;-fx-font-weight:bold;"
                        : "-fx-text-fill:#333;");
            }
        };
    }

    private <T> TableCell<RendezVous, T> createStatutCell() {
        return new TableCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                String color = switch (item.toString()) {
                    case "Confirme"  -> "#1ea064";
                    case "Termine"   -> "#3a7bd5";
                    case "Annule"    -> "#e53935";
                    default          -> "#e09030";
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        };
    }

    // ══ Etat formulaire ═══════════════════════════════════════
    private void activerFormulaire() {
        btnModifier.setDisable(false);
        btnSupprimer.setDisable(false);
        btnVider.setDisable(false);
        fieldStatut.setDisable(false);
        fieldNotes.setDisable(false);
    }
    private void desactiverFormulaire() {
        btnModifier.setDisable(true);
        btnSupprimer.setDisable(true);
        btnVider.setDisable(true);
        fieldStatut.setDisable(true);
        fieldNotes.setDisable(true);
    }

    // ══ Messages ══════════════════════════════════════════════
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
    private String str(String s) { return s != null ? s : ""; }
    private String esc(String s) { return str(s).replace("\"", "'"); }
}