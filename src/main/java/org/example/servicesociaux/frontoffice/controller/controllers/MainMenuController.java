package org.example.servicesociaux.frontoffice.controller.controllers;

import org.example.home.controller.HomeController;
import org.example.servicesociaux.frontoffice.controller.services.HopitalService;
import org.example.servicesociaux.frontoffice.controller.services.RendezVousService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class MainMenuController {

    @FXML private Label countHopital;
    @FXML private Label countRdv;
    @FXML private Label countEnAttente;
    @FXML private Label countDossiers;
    @FXML private LineChart<String, Number> lineChart;

    private final HopitalService    hopitalService = new HopitalService();
    private final RendezVousService rdvService     = new RendezVousService();

    @FXML
    public void initialize() {
        chargerStats();
        chargerLineChart();
        chargerCountDossiers();
    }

    // ══ NAVIGATION ═══════════════════════════════════════════
    @FXML
    public void ouvrirHopital() {
        loadIntoHome("/servicesociaux/frontoffice/hopital.fxml");
    }

    @FXML
    public void ouvrirRendezVous() {
        loadIntoHome("/servicesociaux/frontoffice/rendezVous.fxml");
    }

    @FXML
    public void ouvrirDossierMedical() {
        loadIntoHome("/servicesociaux/frontoffice/dossierMedical.fxml");
    }

    private void loadIntoHome(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("FXML introuvable : " + fxmlPath);
                return;
            }
            Parent view = new FXMLLoader(url).load();
            HomeController.navigateTo(view);
        } catch (Exception e) {
            System.err.println("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══ STATS ════════════════════════════════════════════════
    private void chargerStats() {
        try {
            countHopital.setText(String.valueOf(hopitalService.afficherTous().size()));
        } catch (Exception e) {
            countHopital.setText("—");
        }
        try {
            List<Object[]> rows = hopitalService.afficherAvecRendezVous();
            int totalRdv  = rows.stream().mapToInt(r -> (int) r[6]).sum();
            int enAttente = rows.stream().mapToInt(r -> (int) r[7]).sum();
            countRdv.setText(String.valueOf(totalRdv));
            countEnAttente.setText(String.valueOf(enAttente));
        } catch (Exception e) {
            countRdv.setText("—");
            countEnAttente.setText("—");
        }
    }

    // ══ DOSSIERS MÉDICAUX ════════════════════════════════════
    private void chargerCountDossiers() {
        try {
            org.example.user.service.ServiceDossierMedical dm =
                    new org.example.user.service.ServiceDossierMedical();
            countDossiers.setText(String.valueOf(dm.countDossiers()));
        } catch (Exception e) {
            countDossiers.setText("—");
        }
    }

    // ══ LINECHART ════════════════════════════════════════════
    private void chargerLineChart() {
        try {
            List<Object[]> rows = hopitalService.afficherAvecRendezVous();

            XYChart.Series<String, Number> serieTotal     = new XYChart.Series<>();
            XYChart.Series<String, Number> serieEnAttente = new XYChart.Series<>();
            serieTotal.setName("Total RDV");
            serieEnAttente.setName("En attente");

            for (Object[] r : rows) {
                String nom       = r[1] != null ? (String) r[1] : "?";
                int    nbRdv     = (int) r[6];
                int    enAttente = (int) r[7];
                String label     = nom.length() > 10
                        ? nom.substring(0, 10) + "…" : nom;
                serieTotal.getData().add(new XYChart.Data<>(label, nbRdv));
                serieEnAttente.getData().add(new XYChart.Data<>(label, enAttente));
            }

            lineChart.getData().clear();
            lineChart.getData().addAll(serieTotal, serieEnAttente);
            lineChart.setLegendVisible(true);

            lineChart.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.getRoot().applyCss();
                    lineChart.lookupAll(".default-color0.chart-series-line")
                            .forEach(n -> n.setStyle("-fx-stroke: #ad1457; -fx-stroke-width: 2px;"));
                    lineChart.lookupAll(".default-color0.chart-line-symbol")
                            .forEach(n -> n.setStyle(
                                    "-fx-background-color: #ad1457, white; -fx-background-radius: 5px;"));
                    lineChart.lookupAll(".default-color1.chart-series-line")
                            .forEach(n -> n.setStyle("-fx-stroke: #c9a0c8; -fx-stroke-width: 2px;"));
                    lineChart.lookupAll(".default-color1.chart-line-symbol")
                            .forEach(n -> n.setStyle(
                                    "-fx-background-color: #c9a0c8, white; -fx-background-radius: 5px;"));
                }
            });

        } catch (SQLException e) {
            System.err.println("Erreur lineChart : " + e.getMessage());
        }
    }
}