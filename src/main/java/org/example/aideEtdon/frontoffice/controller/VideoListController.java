package org.example.aideEtdon.frontoffice.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.example.aideEtdon.model.Video;
import org.example.aideEtdon.service.VideoService;

import java.io.IOException;
import java.util.List;

public class VideoListController {

    @FXML private FlowPane videosContainer;
    private VideoService videoService;

    @FXML
    public void initialize() {
        videoService = new VideoService();
        loadVideos();
    }

    private void loadVideos() {
        try {
            List<Video> videos = videoService.afficher();
            videosContainer.getChildren().clear();

            for (Video v : videos) {
                VBox card = createVideoCard(v);
                videosContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createVideoCard(Video v) {
        VBox card = new VBox();
        card.setSpacing(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(450);
        card.getStyleClass().add("video-card");
        card.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label(v.getTitle());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI', sans-serif;");
        title.setWrapText(true);

        // Extract true video ID for thumbnail and routing
        String url = v.getYoutubeUrl();
        String videoId = null;
        if (url != null) {
            if (url.contains("v=")) {
                videoId = url.substring(url.indexOf("v=") + 2);
                if(videoId.contains("&")) videoId = videoId.substring(0, videoId.indexOf("&"));
            } else if (url.contains("youtu.be/")) {
                videoId = url.substring(url.indexOf("youtu.be/") + 9);
                if(videoId.contains("?")) videoId = videoId.substring(0, videoId.indexOf("?"));
            } else if (url.contains("embed/")) {
                videoId = url.substring(url.indexOf("embed/") + 6);
                if(videoId.contains("?")) videoId = videoId.substring(0, videoId.indexOf("?"));
            }
        }
        
        if (videoId != null && !videoId.trim().isEmpty()) {
            String thumbUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView();
            try {
                imgView.setImage(new javafx.scene.image.Image(thumbUrl, true));
            } catch(Exception e) {}
            imgView.setFitWidth(400);
            imgView.setFitHeight(225);
            
            javafx.scene.layout.StackPane videoThumbnailBtn = new javafx.scene.layout.StackPane(imgView);
            videoThumbnailBtn.setCursor(javafx.scene.Cursor.HAND);
            videoThumbnailBtn.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
            
            javafx.scene.control.Button playBtn = new javafx.scene.control.Button("▶ Lancer sur le Navigateur");
            playBtn.setStyle("-fx-background-color: rgba(220, 38, 38, 0.95); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
            playBtn.setMouseTransparent(true); // Let stack pane handle click
            videoThumbnailBtn.getChildren().add(playBtn);
            
            final String finalId = videoId;
            videoThumbnailBtn.setOnMouseClicked(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.youtube.com/watch?v=" + finalId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            
            card.getChildren().addAll(title, videoThumbnailBtn);
        } else {
            Label errorLabel = new Label("⚠️ Lien YouTube invalide");
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            card.getChildren().addAll(title, errorLabel);
        }

        return card;
    }

    @FXML
    public void handleRetour() {
        AideEtdonControllerClientController.getInstance().showDons();
    }
}
