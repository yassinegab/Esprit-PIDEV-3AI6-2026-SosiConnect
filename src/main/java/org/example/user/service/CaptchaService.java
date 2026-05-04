package org.example.user.service;

import com.sun.net.httpserver.HttpServer;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.config.AppConfig;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CaptchaService {

    private static HttpServer server;
    private static int captchaPort = -1;

    private String token;

    public boolean verifyWithDialog() {
        token = null;

        try {
            startLocalServer();

            Stage stage = new Stage();
            stage.setTitle("Vérification reCAPTCHA");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            Label titleLabel = new Label("Vérification de sécurité");
            titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label subtitleLabel = new Label("Cochez « Je ne suis pas un robot », puis cliquez sur Valider.");
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

            Label iconLabel = new Label("🛡");
            iconLabel.setStyle(
                    "-fx-font-size: 30px;" +
                            "-fx-background-color: #fee2e2;" +
                            "-fx-text-fill: #dc2626;" +
                            "-fx-background-radius: 40;" +
                            "-fx-padding: 10 14;"
            );

            HBox headerBox = new HBox(14, iconLabel, new VBox(4, titleLabel, subtitleLabel));
            headerBox.setAlignment(Pos.CENTER_LEFT);

            WebView webView = new WebView();
            webView.setPrefSize(500, 420);

            WebEngine engine = webView.getEngine();
            engine.setJavaScriptEnabled(true);

            Label statusLabel = new Label("Chargement du reCAPTCHA...");
            statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Button verifyButton = new Button("Valider");
            verifyButton.setPrefWidth(500);
            verifyButton.setDisable(true);
            verifyButton.setStyle(
                    "-fx-background-color: #dc2626;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 16px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 12;" +
                            "-fx-padding: 12 20;" +
                            "-fx-cursor: hand;"
            );

            Button cancelButton = new Button("Annuler");
            cancelButton.setPrefWidth(500);
            cancelButton.setStyle(
                    "-fx-background-color: #e5e7eb;" +
                            "-fx-text-fill: #111827;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 12;" +
                            "-fx-padding: 10 20;" +
                            "-fx-cursor: hand;"
            );

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    statusLabel.setText("reCAPTCHA prêt.");
                    verifyButton.setDisable(false);
                } else if (newState == Worker.State.FAILED) {
                    statusLabel.setText("Impossible de charger reCAPTCHA.");
                    verifyButton.setDisable(true);
                }
            });

            verifyButton.setOnAction(e -> {
                try {
                    Object result = engine.executeScript("grecaptcha.getResponse()");
                    token = result == null ? "" : result.toString();

                    if (token.isBlank()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("reCAPTCHA");
                        alert.setHeaderText(null);
                        alert.setContentText("Veuillez d'abord cocher « Je ne suis pas un robot ».");
                        alert.showAndWait();
                        return;
                    }

                    stage.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Erreur JavaScript reCAPTCHA", ex.getMessage());
                }
            });

            cancelButton.setOnAction(e -> {
                token = null;
                stage.close();
            });

            VBox root = new VBox(14, headerBox, webView, statusLabel, verifyButton, cancelButton);
            root.setPadding(new Insets(22));
            root.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #fecaca;" +
                            "-fx-border-width: 1;" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-radius: 20;"
            );

            stage.setScene(new Scene(root, 550, 650));

            engine.load("http://localhost:" + captchaPort + "/recaptcha?t=" + System.currentTimeMillis());

            stage.showAndWait();

            if (token == null || token.isBlank()) {
                return false;
            }

            return verifyTokenWithGoogle(token);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur reCAPTCHA", e.getMessage());
            return false;
        }
    }

    private static synchronized void startLocalServer() throws Exception {
        if (server != null) {
            return;
        }

        // port 0 = Java choisit automatiquement un port libre
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        captchaPort = server.getAddress().getPort();

        server.createContext("/recaptcha", exchange -> {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <script src="https://www.google.com/recaptcha/api.js" async defer></script>
                        <style>
                            html, body {
                                margin: 0;
                                padding: 0;
                                width: 500px;
                                height: 420px;
                                overflow: hidden;
                                background: white;
                                font-family: Arial, sans-serif;
                            }
                            .captcha-box {
                                width: 500px;
                                height: 420px;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                overflow: hidden;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="captcha-box">
                            <div class="g-recaptcha" data-sitekey="%s"></div>
                        </div>
                    </body>
                    </html>
                    """.formatted(AppConfig.RECAPTCHA_SITE_KEY.trim());

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.start();
    }

    private boolean verifyTokenWithGoogle(String responseToken) {
        try {
            String params =
                    "secret=" + URLEncoder.encode(AppConfig.RECAPTCHA_SECRET_KEY.trim(), StandardCharsets.UTF_8) +
                            "&response=" + URLEncoder.encode(responseToken, StandardCharsets.UTF_8);

            URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }

            String jsonText = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonText);

            return json.optBoolean("success", false);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur verification Google reCAPTCHA", e.getMessage());
            return false;
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("reCAPTCHA");
        alert.setHeaderText(header);
        alert.setContentText(message == null ? "Erreur inconnue." : message);
        alert.showAndWait();
    }
}