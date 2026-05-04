package org.example.aideEtdon.backoffice;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.aideEtdon.model.Demande;
import org.example.aideEtdon.model.Video;
import org.example.aideEtdon.model.MapLocation;
import org.example.aideEtdon.model.Alerte;
import org.example.aideEtdon.service.DemandeService;
import org.example.aideEtdon.service.VideoService;
import org.example.aideEtdon.service.MapLocationService;
import org.example.aideEtdon.service.AlerteService;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

import java.util.List;
import javafx.collections.FXCollections;

public class AideEtdonControllerAdmin {

    @FXML private VBox adminDemandesContainer;
    @FXML private VBox adminVideosContainer;
    
    @FXML private TextField videoTitleField;
    @FXML private TextField videoUrlField;
    @FXML private Label videoErrorLabel;

    @FXML private VBox adminMapContainer;
    @FXML private VBox mapLocationsContainer;
    @FXML private TextField locNameField;
    @FXML private TextField locLatField;
    @FXML private TextField locLngField;
    @FXML private ComboBox<String> locTypeBox;
    @FXML private Label locErrorLabel;
    
    private JavaAdminConnector mapConnector = new JavaAdminConnector();

    // Master Navigation
    @FXML private Button btnMasterAide;
    @FXML private Button btnMasterDon;
    @FXML private Button btnMasterStats;
    @FXML private VBox masterAidePane;
    @FXML private VBox masterDonPane;
    @FXML private VBox masterStatsPane;
    @FXML private VBox statsContentBox;

    // Inner Navigation
    @FXML private Button btnViewAide;
    @FXML private Button btnViewDon;
    @FXML private VBox demandesViewBox;
    @FXML private VBox videosViewBox;

    private DemandeService demandeService;
    private VideoService videoService;
    private MapLocationService mapLocationService;
    private AlerteService alerteService;

    @FXML
    public void initialize() {
        demandeService = new DemandeService();
        videoService = new VideoService();
        mapLocationService = new MapLocationService();
        alerteService = new AlerteService();
        
        if (locTypeBox != null) {
            locTypeBox.setItems(FXCollections.observableArrayList("Pharmacie", "Urgence", "Hôpital"));
        }
        
        loadMapLocations();
        initAdminMap();
        
        // Setup initial default views
        showMasterDon();
        showDemandesView();
    }

    @FXML
    private void showMasterAide() {
        if (!btnMasterAide.getStyleClass().contains("active")) btnMasterAide.getStyleClass().add("active");
        btnMasterDon.getStyleClass().remove("active");
        btnMasterStats.getStyleClass().remove("active");

        switchView(masterAidePane, masterDonPane, masterStatsPane);
    }

    @FXML
    private void showMasterDon() {
        if (!btnMasterDon.getStyleClass().contains("active")) btnMasterDon.getStyleClass().add("active");
        btnMasterAide.getStyleClass().remove("active");
        btnMasterStats.getStyleClass().remove("active");

        switchView(masterDonPane, masterAidePane, masterStatsPane);
    }

    @FXML
    private void showMasterStats() {
        if (!btnMasterStats.getStyleClass().contains("active")) btnMasterStats.getStyleClass().add("active");
        btnMasterAide.getStyleClass().remove("active");
        btnMasterDon.getStyleClass().remove("active");

        switchView(masterStatsPane, masterAidePane, masterDonPane);
        loadStatistics();
    }

    @FXML
    private void showDemandesView() {
        if (!btnViewAide.getStyleClass().contains("active")) {
            btnViewAide.getStyleClass().add("active");
        }
        btnViewDon.getStyleClass().remove("active");
        
        switchView(demandesViewBox, videosViewBox);
        loadDemandes();
    }

    @FXML
    private void showVideosView() {
        if (!btnViewDon.getStyleClass().contains("active")) {
            btnViewDon.getStyleClass().add("active");
        }
        btnViewAide.getStyleClass().remove("active");
        
        switchView(videosViewBox, demandesViewBox);
        loadVideos();
    }

    private void switchView(VBox show, VBox... hide) {
        if (show.isVisible()) {
            boolean othersHidden = true;
            for (VBox h : hide) if (h.isVisible()) { othersHidden = false; break; }
            if (othersHidden) return;
        }

        for (VBox h : hide) h.setVisible(false);
        show.setVisible(true);
        show.setOpacity(0);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(450), show);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(450), show);
        tt.setFromY(25);
        tt.setToY(0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
        pt.play();
    }

    private void loadDemandes() {
        try {
            List<Demande> demandes = demandeService.afficher();
            adminDemandesContainer.getChildren().clear();

            for (Demande d : demandes) {
                HBox row = new HBox(20);
                row.getStyleClass().add("admin-row");
                row.setAlignment(Pos.CENTER_LEFT);

                VBox textBox = new VBox(6);
                Label titleLbl = new Label(d.getTitre());
                titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
                Label descLbl = new Label(d.getDescription());
                descLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");
                descLbl.setWrapText(true);
                descLbl.setMaxWidth(360);
                textBox.getChildren().addAll(titleLbl, descLbl);

                Label typeLabel = new Label(d.getType());
                typeLabel.getStyleClass().add("info-badge");

                Label badge = new Label(d.getUrgence());
                badge.getStyleClass().add("Urgent".equalsIgnoreCase(d.getUrgence()) ? "urgent-badge" : "modern-badge");

                Button btnDel = new Button("Supprimer");
                btnDel.getStyleClass().add("danger-button");
                btnDel.setOnAction(e -> {
                    try {
                        demandeService.supprimer(d.getId());
                        loadDemandes();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                javafx.scene.layout.HBox buttonsBox = new javafx.scene.layout.HBox(10);
                buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                buttonsBox.getChildren().add(btnDel);

                if ("Sang".equalsIgnoreCase(d.getType()) && d.getGroupeSanguin() != null && !d.getGroupeSanguin().trim().isEmpty()) {
                    Button btnMatch = new Button("🎯 Matching");
                    btnMatch.getStyleClass().add("primary-admin-button");
                    btnMatch.setOnAction(e -> {
                        btnMatch.setDisable(true);
                        btnMatch.setText("Recherche IA...");
                        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                org.example.aideEtdon.service.MatchingService ms = new org.example.aideEtdon.service.MatchingService();
                                List<org.example.user.model.User> matchs = ms.runBloodMatch(d);

                                if (matchs.isEmpty()) {
                                    javafx.application.Platform.runLater(() -> {
                                        btnMatch.setText("Aucun Match");
                                        btnMatch.setStyle("-fx-background-color: #94a3b8; -fx-text-fill: white;");
                                        
                                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                                        alert.setTitle("Matching Médical");
                                        alert.setHeaderText("Aucun profil compatible");
                                        alert.setContentText("Aucun donneur historique correspondant n'a été trouvé.");
                                        alert.show();
                                    });
                                    return null;
                                }

                                StringBuilder sb = new StringBuilder();
                                for (org.example.user.model.User u : matchs) {
                                    sb.append("- ").append(u.getNom()).append(" ").append(u.getPrenom())
                                      .append("\n  Email : ").append(u.getEmail()).append("\n\n");
                                }
                                
                                javafx.application.Platform.runLater(() -> {
                                    btnMatch.setText("🎯 " + matchs.size() + " Trouvé(s)!");
                                    btnMatch.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
                                    
                                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                                    alert.setTitle("Matching Médical Réussi");
                                    alert.setHeaderText(matchs.size() + " Donneur(s) Compatible(s) Trouvé(s)");
                                    alert.setContentText(sb.toString().trim());
                                    alert.show();
                                });
                                return null;
                            }
                        };
                        new Thread(task).start();
                    });
                    buttonsBox.getChildren().add(0, btnMatch);
                }

                HBox.setHgrow(textBox, Priority.ALWAYS);
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                row.getChildren().addAll(textBox, typeLabel, badge, spacer, buttonsBox);
                adminDemandesContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStatistics() {
        if (statsContentBox == null) return;
        try {
            statsContentBox.getChildren().clear();

            List<Demande> demandes = demandeService.afficher();
            List<Video> videos = videoService.afficher();
            List<MapLocation> locations = mapLocationService.afficher();
            List<Alerte> alertes = alerteService.afficher();

            long urgentCount = demandes.stream().filter(d -> "Urgent".equalsIgnoreCase(d.getUrgence())).count();
            long normalCount = demandes.size() - urgentCount;
            long enAttente = alertes.stream().filter(a -> "En Attente".equals(a.getStatut())).count();
            long resolues = alertes.size() - enAttente;

            // === Insight Row ===
            HBox insightRow = new HBox(10);
            insightRow.setAlignment(Pos.CENTER_LEFT);
            insightRow.getStyleClass().add("insight-row");
            Label insight = new Label("Vue d'ensemble");
            insight.getStyleClass().add("insight-accent");
            Label insightDetail = new Label(demandes.size() + " demandes actives, " + alertes.size() + " alertes enregistrees, " + locations.size() + " points de service cartographies.");
            insightDetail.getStyleClass().add("insight-text");
            Region igrow = new Region();
            HBox.setHgrow(igrow, Priority.ALWAYS);
            insightRow.getChildren().addAll(insight, insightDetail, igrow);
            statsContentBox.getChildren().add(insightRow);

            // === Stat Cards Row ===
            javafx.scene.layout.FlowPane statCards = new javafx.scene.layout.FlowPane();
            statCards.setHgap(18);
            statCards.setVgap(18);
            statCards.setPrefWrapLength(900);

            javafx.scene.Node[] cards = new javafx.scene.Node[] {
                createStatCard(String.valueOf(demandes.size()), "Demandes", "stats-card stats-card-teal", "stat-bar-fill-teal", "D", "#0f766e"),
                createStatCard(String.valueOf(urgentCount), "Urgentes", "stats-card stats-card-red", "stat-bar-fill-red", "!", "#b91c1c"),
                createStatCard(String.valueOf(alertes.size()), "Alertes", "stats-card stats-card-amber", "stat-bar-fill-amber", "A", "#b45309"),
                createStatCard(String.valueOf(locations.size()), "Points Map", "stats-card stats-card-emerald", "stat-bar-fill-emerald", "M", "#047857"),
                createStatCard(String.valueOf(videos.size()), "Videos", "stats-card stats-card-blue", "stat-bar-fill-blue", "V", "#1d4ed8")
            };
            for (int i = 0; i < cards.length; i++) {
                javafx.scene.Node c = cards[i];
                c.setOpacity(0);
                c.setTranslateY(20);
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), c);
                ft.setToValue(1);
                ft.setDelay(javafx.util.Duration.millis(i * 80));
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(400), c);
                tt.setToY(0);
                tt.setDelay(javafx.util.Duration.millis(i * 80));
                ft.play();
                tt.play();
                statCards.getChildren().add(c);
            }
            statsContentBox.getChildren().add(statCards);

            // === Demandes Section ===
            statsContentBox.getChildren().add(createSectionCard("Repartition des Demandes",
                createDistributionBar("Urgent", urgentCount, demandes.size(), "stat-bar-fill-red"),
                createDistributionBar("Normal", normalCount, demandes.size(), "stat-bar-fill-teal")
            ));

            // === Alertes Section ===
            statsContentBox.getChildren().add(createSectionCard("Statut des Alertes",
                createDistributionBar("En Attente", enAttente, alertes.size(), "stat-bar-fill-amber"),
                createDistributionBar("Resolues", resolues, alertes.size(), "stat-bar-fill-emerald")
            ));

            // === Type Distribution Section ===
            java.util.Map<String, Long> typeCounts = demandes.stream()
                .collect(java.util.stream.Collectors.groupingBy(Demande::getType, java.util.stream.Collectors.counting()));
            if (!typeCounts.isEmpty()) {
                String[] barStyles = {"stat-bar-fill-teal", "stat-bar-fill-blue", "stat-bar-fill-amber", "stat-bar-fill-red", "stat-bar-fill-emerald"};
                int[] styleIdx = {0};
                java.util.List<javafx.scene.Node> typeBars = new java.util.ArrayList<>();
                typeCounts.forEach((type, count) -> {
                    String style = barStyles[styleIdx[0] % barStyles.length];
                    typeBars.add(createDistributionBar(type, count, demandes.size(), style));
                    styleIdx[0]++;
                });
                statsContentBox.getChildren().add(createSectionCard("Types de Demandes", typeBars.toArray(new javafx.scene.Node[0])));
            }

            // === Blood Type Section ===
            java.util.Map<String, Long> bloodCounts = demandes.stream()
                .filter(d -> d.getGroupeSanguin() != null && !d.getGroupeSanguin().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(Demande::getGroupeSanguin, java.util.stream.Collectors.counting()));
            if (!bloodCounts.isEmpty()) {
                String[] barStyles = {"stat-bar-fill-teal", "stat-bar-fill-blue", "stat-bar-fill-amber", "stat-bar-fill-red", "stat-bar-fill-emerald"};
                int[] styleIdx = {0};
                java.util.List<javafx.scene.Node> bloodBars = new java.util.ArrayList<>();
                bloodCounts.forEach((type, count) -> {
                    String style = barStyles[styleIdx[0] % barStyles.length];
                    bloodBars.add(createDistributionBar(type, count, demandes.size(), style));
                    styleIdx[0]++;
                });
                statsContentBox.getChildren().add(createSectionCard("Groupes Sanguins Demandes", bloodBars.toArray(new javafx.scene.Node[0])));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createStatCard(String number, String label, String cardStyle, String accentStyle, String iconText, String iconColor) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll(cardStyle.split(" "));
        card.setPrefWidth(170);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Top row: icon badge + number
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(iconText);
        iconLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + iconColor + "; -fx-font-family: 'Segoe UI', sans-serif;");
        iconLbl.setAlignment(javafx.geometry.Pos.CENTER);
        iconLbl.setMinSize(48, 48);
        iconLbl.setMaxSize(48, 48);
        iconLbl.setPrefSize(48, 48);

        String badgeStyle = accentStyle.replace("stat-bar-fill", "stat-icon-badge");
        iconLbl.getStyleClass().addAll("stat-icon-badge", badgeStyle);

        VBox numBox = new VBox(2);
        Label numLbl = new Label(number);
        numLbl.getStyleClass().add("stats-number");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stats-label");
        numBox.getChildren().addAll(numLbl, lbl);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        top.getChildren().addAll(iconLbl, numBox, grow);

        card.getChildren().add(top);
        return card;
    }

    private VBox createSectionCard(String title, javafx.scene.Node... children) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-section-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif; -fx-padding: 0 0 6 0;");

        Region divider = new Region();
        divider.getStyleClass().add("section-divider");
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setPrefHeight(1);

        card.getChildren().add(lbl);
        card.getChildren().add(divider);
        card.getChildren().addAll(children);
        return card;
    }

    private VBox createDistributionBar(String label, long count, long total, String fillStyle) {
        VBox box = new VBox(6);
        box.setStyle("-fx-padding: 0 0 6 0;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(label);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #475569; -fx-font-family: 'Segoe UI', sans-serif;");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        int pct = total > 0 ? (int) Math.round(count * 100.0 / total) : 0;
        Label pctLbl = new Label(String.valueOf(count));
        pctLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
        Label pctSmall = new Label("(" + pct + "%)");
        pctSmall.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI', sans-serif;");
        top.getChildren().addAll(name, grow, pctLbl, pctSmall);

        // Bar track
        Region track = new Region();
        track.getStyleClass().add("stat-bar-track");
        track.setPrefHeight(10);
        track.setMaxWidth(Double.MAX_VALUE);
        track.setMaxHeight(10);

        // Bar fill (overlaid)
        javafx.scene.layout.StackPane barStack = new javafx.scene.layout.StackPane();
        barStack.setAlignment(Pos.CENTER_LEFT);
        barStack.setMaxWidth(Double.MAX_VALUE);
        barStack.setMaxHeight(10);
        Region fill = new Region();
        fill.getStyleClass().addAll("stat-bar-fill", fillStyle);
        double pctWidth = total > 0 ? (count * 100.0 / total) : 0;
        fill.prefWidthProperty().bind(barStack.widthProperty().multiply(pctWidth / 100.0));
        fill.setPrefHeight(10);
        fill.setMaxHeight(10);
        barStack.getChildren().addAll(track, fill);

        box.getChildren().addAll(top, barStack);
        return box;
    }

    private void loadVideos() {
        try {
            List<Video> videos = videoService.afficher();
            adminVideosContainer.getChildren().clear();

            for (Video v : videos) {
                HBox row = new HBox(15);
                row.getStyleClass().add("admin-row");
                row.setAlignment(Pos.CENTER_LEFT);

                Label info = new Label(v.getTitle());
                info.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
                Label url = new Label(v.getYoutubeUrl());
                url.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");
                url.setWrapText(true);
                url.setMaxWidth(300);

                VBox infoBox = new VBox(5, info, url);

                Button btnDel = new Button("Supprimer");
                btnDel.getStyleClass().add("danger-button");
                btnDel.setOnAction(e -> {
                    try {
                        videoService.supprimer(v.getId());
                        loadVideos(); 
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                row.getChildren().addAll(infoBox, spacer, btnDel);
                adminVideosContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddVideo() {
        videoErrorLabel.setVisible(false);
        String title = videoTitleField.getText().trim();
        String url = videoUrlField.getText().trim();

        if (title.isEmpty() || url.isEmpty()) {
            videoErrorLabel.setVisible(true);
            return;
        }

        try {
            Video v = new Video(title, url);
            videoService.ajouter(v);
            
            videoTitleField.clear();
            videoUrlField.clear();
            loadVideos(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMapLocations() {
        if (mapLocationsContainer == null) return;
        try {
            List<MapLocation> locs = mapLocationService.afficher();
            mapLocationsContainer.getChildren().clear();
            for (MapLocation m : locs) {
                HBox row = new HBox(15);
                row.getStyleClass().add("admin-row");
                row.setAlignment(Pos.CENTER_LEFT);

                VBox infoBox = new VBox(5);
                Label nameLbl = new Label(m.getName());
                nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
                Label coords = new Label("Lat: " + m.getLatitude() + " | Lng: " + m.getLongitude());
                coords.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-family: 'Segoe UI', sans-serif;");
                infoBox.getChildren().addAll(nameLbl, coords);

                Label typeLbl = new Label(m.getType());
                if ("Urgence".equals(m.getType())) {
                    typeLbl.getStyleClass().add("urgent-badge");
                } else if ("Hôpital".equals(m.getType())) {
                    typeLbl.getStyleClass().add("info-badge");
                } else {
                    typeLbl.getStyleClass().add("modern-badge");
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button btnDel = new Button("Supprimer");
                btnDel.getStyleClass().add("danger-button");
                btnDel.setOnAction(e -> {
                    mapLocationService.supprimer(m.getId());
                    loadMapLocations();
                });

                row.getChildren().addAll(infoBox, typeLbl, spacer, btnDel);
                mapLocationsContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddMapLocation() {
        if (locErrorLabel == null) return;
        locErrorLabel.setVisible(false);
        String name = locNameField.getText().trim();
        String latStr = locLatField.getText().trim();
        String lngStr = locLngField.getText().trim();
        String type = locTypeBox.getValue();

        if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty() || type == null) {
            locErrorLabel.setText("Tous les champs sont requis!");
            locErrorLabel.setVisible(true);
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);
            MapLocation m = new MapLocation(name, type, lat, lng);
            mapLocationService.ajouter(m);

            locNameField.clear();
            locLatField.clear();
            locLngField.clear();
            locTypeBox.getSelectionModel().clearSelection();
            loadMapLocations();
        } catch (NumberFormatException e) {
            locErrorLabel.setText("Lat/Lng invalides!");
            locErrorLabel.setVisible(true);
        }
    }

    public class JavaAdminConnector {
        public void setCoordinates(double lat, double lng) {
            javafx.application.Platform.runLater(() -> {
                if (locLatField != null && locLngField != null) {
                    locLatField.setText(String.format(java.util.Locale.US, "%.6f", lat));
                    locLngField.setText(String.format(java.util.Locale.US, "%.6f", lng));
                }
            });
        }
    }

    private void initAdminMap() {
        if (adminMapContainer != null) {
            adminMapContainer.getChildren().clear();
            WebView mapWebView = new WebView();
            mapWebView.setMinHeight(350);
            javafx.scene.layout.VBox.setVgrow(mapWebView, Priority.ALWAYS);
            adminMapContainer.getChildren().add(mapWebView);
            
            WebEngine webEngine = mapWebView.getEngine();
            webEngine.setJavaScriptEnabled(true);
            
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject) webEngine.executeScript("window");
                    window.setMember("javaConnector", mapConnector);
                }
            });
            
            StringBuilder jsonBuilder = new StringBuilder("[");
            List<org.example.aideEtdon.model.MapLocation> locations = mapLocationService.afficher();
            for (int i = 0; i < locations.size(); i++) {
                org.example.aideEtdon.model.MapLocation loc = locations.get(i);
                jsonBuilder.append("{")
                        .append("\"lat\":").append(loc.getLatitude()).append(",")
                        .append("\"lng\":").append(loc.getLongitude()).append(",")
                        .append("\"name\":\"").append(loc.getName().replace("\"", "\\\"")).append("\",")
                        .append("\"type\":\"").append(loc.getType().replace("\"", "\\\"")).append("\"")
                        .append("}");
                if (i < locations.size() - 1) jsonBuilder.append(",");
            }
            jsonBuilder.append("]");

            String htmlContent = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Admin Map Picker</title>
                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                        <script>window.L_DISABLE_3D = true;</script>
                        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                        <style>
                            body, html { margin: 0; padding: 0; width: 100%%; height: 100%%; overflow: hidden; }
                            #map { position: absolute; top: 0; bottom: 0; left: 0; right: 0; border-radius: 8px; }
                        </style>
                    </head>
                    <body>
                    <div id="map"></div>
                    <script>
                        try {
                            var map = L.map('map', {
                                zoomAnimation: false, fadeAnimation: false, markerZoomAnimation: false,
                                minZoom: 5, maxZoom: 19
                            }).setView([36.8065, 10.1815], 13);
                            
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; OpenStreetMap', noWrap: true
                            }).addTo(map);
                            
                            var dbLocations = %s;
                            dbLocations.forEach(function(loc) {
                                L.marker([loc.lat, loc.lng]).addTo(map)
                                    .bindPopup("<b>" + loc.name + "</b><br>" + loc.type);
                            });
                            
                            var currentMarker = null;
                            map.on('click', function(e) {
                                if(currentMarker) { map.removeLayer(currentMarker); }
                                currentMarker = L.marker([e.latlng.lat, e.latlng.lng]).addTo(map);
                                if (window.javaConnector) { window.javaConnector.setCoordinates(e.latlng.lat, e.latlng.lng); }
                            });
                            
                            setTimeout(function() { map.invalidateSize({pan: false}); }, 450);
                        } catch(e) { console.error(e); }
                    </script>
                    </body>
                    </html>
                    """.formatted(jsonBuilder.toString());
            webEngine.loadContent(htmlContent);
        }
    }
}

