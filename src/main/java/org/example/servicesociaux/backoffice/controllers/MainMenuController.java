package org.example.servicesociaux.backoffice.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.example.backoffice.controller.AdminBaseController;
import org.example.servicesociaux.backoffice.services.HopitalService;
import org.example.servicesociaux.backoffice.services.RendezVousService;

import java.sql.SQLException;
import java.util.List;

/**
 * MainMenuController — Backoffice Services Sociaux.
 *
 * ✅ FIX : Implémente AdminAware pour recevoir AdminBaseController par injection.
 *          Navigation via adminController.loadView() au lieu du singleton statique.
 *
 * Problème corrigé :
 *   Avant → AdminBaseController.navigateTo(view) utilisait le singleton statique.
 *           Après un retour vers MainMenu, le singleton était mis à jour mais
 *           les sous-pages (Hôpital, RDV) gardaient une ancienne référence.
 *
 *   Après → adminController est injecté à chaque chargement de MainMenu.
 *            La référence est toujours fraîche et correcte.
 */
public class MainMenuController implements AdminBaseController.AdminAware {

    @FXML private Label                    countHopital;
    @FXML private Label                    countRdv;
    @FXML private Label                    countEnAttente;
    @FXML private BarChart<String, Number> barChart;

    private final HopitalService    hopitalService = new HopitalService();
    private final RendezVousService rdvService     = new RendezVousService();

    // ✅ Référence injectée par AdminBaseController.loadView()
    private AdminBaseController adminController;

    @Override
    public void setAdminController(AdminBaseController admin) {
        this.adminController = admin;
    }

    // ══ Init ══════════════════════════════════════════════════
    @FXML
    public void initialize() {
        chargerStats();
        chargerBarChart();
    }

    // ══ Stats ═════════════════════════════════════════════════
    private void chargerStats() {
        try {
            countHopital.setText(
                    String.valueOf(hopitalService.afficherTous().size()));
        } catch (Exception e) { countHopital.setText("—"); }

        try {
            List<Object[]> rows = hopitalService.afficherAvecRendezVous();
            countRdv      .setText(String.valueOf(
                    rows.stream().mapToInt(r -> (int) r[6]).sum()));
            countEnAttente.setText(String.valueOf(
                    rows.stream().mapToInt(r -> (int) r[7]).sum()));
        } catch (Exception e) {
            countRdv.setText("—");
            countEnAttente.setText("—");
        }
    }

    // ══ BarChart ══════════════════════════════════════════════
    private void chargerBarChart() {
        try {
            List<Object[]> rows = hopitalService.afficherAvecRendezVous();

            XYChart.Series<String, Number> serieTotal     = new XYChart.Series<>();
            XYChart.Series<String, Number> serieEnAttente = new XYChart.Series<>();
            serieTotal    .setName("Total RDV");
            serieEnAttente.setName("En attente");

            for (Object[] r : rows) {
                String nom   = r[1] != null ? (String) r[1] : "?";
                String label = nom.length() > 10
                        ? nom.substring(0, 10) + "…" : nom;
                serieTotal    .getData().add(new XYChart.Data<>(label, (int) r[6]));
                serieEnAttente.getData().add(new XYChart.Data<>(label, (int) r[7]));
            }

            barChart.getData().clear();
            barChart.getData().addAll(serieTotal, serieEnAttente);
            barChart.setLegendVisible(true);

            // Couleurs barres
            barChart.sceneProperty().addListener((obs, old, sc) -> {
                if (sc != null) {
                    sc.getRoot().applyCss();
                    barChart.lookupAll(".default-color0.chart-bar")
                            .forEach(n -> n.setStyle("-fx-bar-fill:#f5e6d3;"));
                    barChart.lookupAll(".default-color1.chart-bar")
                            .forEach(n -> n.setStyle("-fx-bar-fill:#e09030;"));
                }
            });

        } catch (SQLException e) {
            System.err.println("Erreur barChart : " + e.getMessage());
        }
    }

    // ══ Navigation ════════════════════════════════════════════

    /**
     * ✅ FIX PRINCIPAL : utilise adminController (injecté) au lieu du singleton.
     *
     * adminController.loadView() :
     *   1. Charge le FXML
     *   2. Injecte this (adminController) dans le controller enfant via AdminAware
     *   3. Affiche la vue dans adminContentArea
     *
     * Résultat : Hôpital et RDV reçoivent la bonne référence et peuvent
     * revenir à MainMenu correctement via retourAccueil().
     */
    @FXML
    public void ouvrirHopital() {
        if (adminController == null) {
            System.err.println("❌ adminController non injecte dans MainMenuController");
            return;
        }
        adminController.loadView(
                "/servicesociaux/backoffice/hopital.fxml", null);
    }

    @FXML
    public void ouvrirRendezVous() {
        if (adminController == null) {
            System.err.println("❌ adminController non injecte dans MainMenuController");
            return;
        }
        adminController.loadView(
                "/servicesociaux/backoffice/rendezVous.fxml", null);
    }
}