package org.example.cycle.frontoffice.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebView;
import org.example.home.controller.HomeController;

import java.io.IOException;

public class GoogleCalendarWebController {

    @FXML private WebView webView;
    @FXML private ProgressIndicator progressIndicator;

    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    public void initialize() {
        progressIndicator.setVisible(true);
        
        // Configurer le WebView
        webView.getEngine().setJavaScriptEnabled(true);
        webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                progressIndicator.setVisible(false);
            }
        });

        // Charger Google Calendar
        webView.getEngine().load("https://calendar.google.com/calendar/u/0/r");
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cycle/frontoffice/DisplayCycle.fxml"));
            Parent view = loader.load();
            DisplayCycleController controller = loader.getController();
            controller.setHomeController(homeController);
            if (homeController != null) {
                homeController.setContent(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
