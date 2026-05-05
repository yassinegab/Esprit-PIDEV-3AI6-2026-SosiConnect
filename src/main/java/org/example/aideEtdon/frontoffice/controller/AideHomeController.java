package org.example.aideEtdon.frontoffice.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.example.aideEtdon.model.ContactUrgence;
import org.example.aideEtdon.model.MapLocation;
import org.example.aideEtdon.service.ContactUrgenceService;
import org.example.aideEtdon.service.MapLocationService;
import org.example.utils.ToastNotification;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class AideHomeController {

    @FXML private Label statusIndicator;
    @FXML private ToggleGroup typeGroup;
    @FXML private ToggleButton btnMed, btnDanger, btnOther;
    @FXML private CheckBox chkLocation;
    @FXML private Button btnEmergency;
    @FXML private ListView<String> historyList;
    @FXML private VBox mapContainer;
    @FXML private ComboBox<String> mapFilterCombo;
    
    private WebView mapWebView;

    private ContactUrgenceService contactService = new ContactUrgenceService();
    private MapLocationService mapLocationService = new MapLocationService();
    private ObservableList<String> history = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        typeGroup = new ToggleGroup();
        btnMed.setToggleGroup(typeGroup);
        btnDanger.setToggleGroup(typeGroup);
        btnOther.setToggleGroup(typeGroup);
        historyList.setItems(history);

        setupToggleStyle(btnMed);
        setupToggleStyle(btnDanger);
        setupToggleStyle(btnOther);
        
        btnMed.setSelected(true); // Default selection
        setupButtonAnimation(btnEmergency);

        btnMed.setTooltip(new javafx.scene.control.Tooltip("Demander des médicaments"));
        btnDanger.setTooltip(new javafx.scene.control.Tooltip("Demander de l'aide pour les courses"));
        btnOther.setTooltip(new javafx.scene.control.Tooltip("Demander une assistance générale"));
        btnEmergency.setTooltip(new javafx.scene.control.Tooltip("Envoyer une alerte d'urgence à vos contacts"));
        
        if (mapFilterCombo != null) {
            mapFilterCombo.setItems(FXCollections.observableArrayList("Tous les services", "Pharmacies Uniquement", "Urgences & Hôpitaux"));
            mapFilterCombo.getSelectionModel().selectFirst();
            mapFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (mapWebView != null && mapWebView.getEngine() != null) {
                    mapWebView.getEngine().executeScript("if(window.filterMarkers) window.filterMarkers('" + newVal.replace("'", "\\'") + "');");
                }
            });
        }
        
        initMap();

        // Staggered entrance animations for panels
        animateEntrance(btnEmergency.getParent(), 0);
        animateEntrance(mapContainer, 150);
        if (historyList != null) animateEntrance(historyList, 300);
    }

    private void animateEntrance(Node node, double delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(25);

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));

        ft.play();
        tt.play();
    }

    private void initMap() {
        if (mapContainer != null) {
            mapContainer.getChildren().clear();
            mapContainer.setMinHeight(600);
            
            mapWebView = new WebView();
            mapWebView.setMinHeight(600);
            mapWebView.setPrefHeight(700);
            mapWebView.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.VBox.setVgrow(mapWebView, javafx.scene.layout.Priority.ALWAYS);
            mapContainer.getChildren().add(mapWebView);
            
            WebEngine webEngine = mapWebView.getEngine();
            webEngine.setJavaScriptEnabled(true);

            // Fetch map data synchronously right now, BEFORE sending it to the layout engine
            List<MapLocation> locationsList = mapLocationService.afficher();
            JSONArray array = new JSONArray();
            for (MapLocation loc : locationsList) {
                JSONObject obj = new JSONObject();
                obj.put("name", loc.getName());
                obj.put("lat", loc.getLatitude());
                obj.put("lng", loc.getLongitude());
                obj.put("type", loc.getType());
                array.put(obj);
            }
            String jsonArrayStr = array.toString();

            String htmlContent = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>SosiConnect Map</title>
                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                        <script>
                            // THE ULTIMATE FIX: Force Software Coordinate Positioning instead of CSS 3D Transforms!
                            // This guarantees JavaFX WebView Prism Composer never scrambles the canvas due to GPU tearing!
                            window.L_DISABLE_3D = true;
                        </script>
                        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                        <style>
                            body, html { margin: 0; padding: 0; width: 100%%; height: 100%%; overflow: hidden; background-color: #f1f5f9; }
                            #map { position: absolute; top: 0; bottom: 0; left: 0; right: 0; border-radius: 12px; }
                            
                        </style>
                    </head>
                    <body>
                    <div id="map"></div>
                    <script>
                        try {
                            var map = L.map('map', {
                                zoomAnimation: false, 
                                fadeAnimation: false, 
                                markerZoomAnimation: false,
                                minZoom: 5,
                                maxZoom: 19
                            }).setView([36.8065, 10.1815], 13);
                            
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; OpenStreetMap',
                                noWrap: true
                            }).addTo(map);
                            
                            var markersLayer = L.layerGroup().addTo(map);
                            var locations = %s;
                            
                            window.filterMarkers = function(filterType) {
                                try {
                                    markersLayer.clearLayers();
                                    var activeBounds = [];
                                    locations.forEach(function(loc) {
                                        var show = false;
                                        if (filterType === 'Tous les services') {
                                            show = true;
                                        } else if (filterType === 'Pharmacies Uniquement' && loc.type.toLowerCase() === 'pharmacie') {
                                            show = true;
                                        } else if (filterType === 'Urgences & Hôpitaux' && (loc.type.toLowerCase() === 'urgence' || loc.type.toLowerCase() === 'hôpital')) {
                                            show = true;
                                        }
                                        
                                        if (show) {
                                            L.marker([loc.lat, loc.lng]).addTo(markersLayer)
                                                .bindPopup("<b>" + loc.name + "</b><br>" + loc.type);
                                            activeBounds.push([loc.lat, loc.lng]);
                                        }
                                    });
                                    if (activeBounds.length > 0) {
                                        map.fitBounds(activeBounds, { padding: [50, 50], maxZoom: 15 });
                                    }
                                } catch(e) { console.error(e); }
                            };
                            
                            // Initialize default view
                            setTimeout(function() {
                                map.invalidateSize({pan: false});
                                window.filterMarkers('Tous les services');
                            }, 450);

                        } catch(e) { 
                            console.error("Map Init Error:", e); 
                        }
                    </script>
                    </body>
                    </html>
                    """.formatted(jsonArrayStr);

            webEngine.loadContent(htmlContent);
        }
    }

    private void setupToggleStyle(ToggleButton btn) {
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-padding: 18 24; -fx-background-radius: 14; -fx-cursor: hand; -fx-text-fill: white; -fx-background-color: linear-gradient(to bottom right, #0d9488, #14b8a6); -fx-border-color: transparent; -fx-border-width: 1px; -fx-border-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(13, 148, 136, 0.30), 14, 0, 0, 5); -fx-scale-x: 1.03; -fx-scale-y: 1.03;");
            } else {
                btn.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-padding: 18 24; -fx-background-radius: 14; -fx-cursor: hand; -fx-text-fill: #475569; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-border-radius: 14; -fx-effect: null; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
            }
        });

        // Tactile press micro-interaction
        btn.setOnMousePressed(e -> {
            if (!btn.isSelected()) {
                btn.setScaleX(0.97);
                btn.setScaleY(0.97);
            }
        });
        btn.setOnMouseReleased(e -> {
            if (!btn.isSelected()) {
                btn.setScaleX(1.0);
                btn.setScaleY(1.0);
            }
        });
    }

    private void setupButtonAnimation(Button btn) {
        javafx.animation.Timeline pulse = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(btn.scaleXProperty(), 1.0),
                new javafx.animation.KeyValue(btn.scaleYProperty(), 1.0)),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1.2),
                new javafx.animation.KeyValue(btn.scaleXProperty(), 1.03),
                new javafx.animation.KeyValue(btn.scaleYProperty(), 1.03)),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2.4),
                new javafx.animation.KeyValue(btn.scaleXProperty(), 1.0),
                new javafx.animation.KeyValue(btn.scaleYProperty(), 1.0))
        );
        pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        btn.setOnMouseEntered(e -> {
            pulse.pause();
            btn.setScaleX(1.05);
            btn.setScaleY(1.05);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
            pulse.play();
        });
    }

    @FXML
    private void handleEmergencyAction() {
        List<ContactUrgence> contacts = contactService.afficherToutes();
        if (contacts.isEmpty()) {
            AideEtdonControllerClientController.getInstance().showToast("⚠️ Aucun contact configuré ! Veuillez d'abord en ajouter.", ToastNotification.ToastType.WARNING, 4.0);
            return;
        }

        java.awt.Toolkit.getDefaultToolkit().beep();

        String type = "Assistance";
        javafx.scene.control.ToggleButton selected = (javafx.scene.control.ToggleButton) typeGroup.getSelectedToggle();
        if (selected != null) type = selected.getText();
        
        final String finalType = type;
        final String time = getCurrentTime();

        String logEntry = "\uD83D\uDCCC AIDE DEMANDÉE: " + finalType + " \u23F0 " + time;
        history.add(0, logEntry);

        statusIndicator.setText("📡 RÉCUPÉRATION GPS...");
        statusIndicator.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #d97706;");

        // Automatic location detection in background thread (Windows Location API + IP fallback)
        new Thread(() -> {
            System.out.println("Resolving location automatically...");
            org.example.aideEtdon.service.GeoLocationService.Coordinate coords =
                    org.example.aideEtdon.service.GeoLocationService.fetchUserLocation();
            System.out.println("Location resolved: " + coords.lat + ", " + coords.lng);

            javafx.application.Platform.runLater(() ->
                    dispatchEmergency(contacts, finalType, time, coords.lat, coords.lng));
        }).start();
    }

    private void dispatchEmergency(List<ContactUrgence> contacts, String type, String time, double lat, double lng) {
        statusIndicator.setText("📡 ENVOI EN COURS...");

        new Thread(() -> {
            // 1. Persist Emergency
            try {
                org.example.aideEtdon.service.AlerteService alertSvc = new org.example.aideEtdon.service.AlerteService();
                alertSvc.ajouter(new org.example.aideEtdon.model.Alerte(type, lat, lng));
                System.out.println("Alerte DB " + type + " enregistrée: " + lat + ", " + lng);
            } catch (Exception ex) {
                System.err.println("Erreur sauvegarde alerte: " + ex.getMessage());
            }

            // 2. Send emails
            System.out.println("====== DÉBUT DE LA DIFFUSION E-MAIL ======");
            for (org.example.aideEtdon.model.ContactUrgence contact : contacts) {
                System.out.println("[Dispatch] Émission vers: " + contact.getEmail());
                org.example.aideEtdon.service.EmailService.sendEmergencyAlert(contact.getEmail(), type, time, lat, lng);
            }

            final int cSize = contacts.size();
            javafx.application.Platform.runLater(() -> {
                AideEtdonControllerClientController.getInstance().showToast(
                    "📨 Alerte diffusée à " + cSize + " contact(s) ! Position GPS: " + String.format("%.4f, %.4f", lat, lng),
                    ToastNotification.ToastType.SUCCESS, 5.0);

                statusIndicator.setText("🚨 AIDE DEMANDÉE");
                statusIndicator.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            });
        }).start();
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @FXML
    private void goToContactForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aideEtdon/frontoffice/ContactFormView.fxml"));
            Node formView = loader.load();
            AideEtdonControllerClientController.getInstance().setView(formView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
