package org.example.user.service;

import com.sun.net.httpserver.HttpServer;
import org.example.config.AppConfig;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class GoogleAuthService {

    public static class GoogleUserInfo {
        private final String email;
        private final String firstName;
        private final String lastName;

        public GoogleUserInfo(String email, String firstName, String lastName) {
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public GoogleUserInfo loginWithGoogle() throws Exception {
        validateConfig();

        String state = generateState();
        String redirectUri = "http://localhost:" + AppConfig.GOOGLE_REDIRECT_PORT + "/oauth2callback";

        BlockingQueue<String> codeQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<String> stateQueue = new ArrayBlockingQueue<>(1);

        HttpServer server = HttpServer.create(new InetSocketAddress(AppConfig.GOOGLE_REDIRECT_PORT), 0);

        server.createContext("/oauth2callback", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String code = getQueryParam(query, "code");
            String returnedState = getQueryParam(query, "state");

            String html =
                    "<html><body style='font-family:Arial;text-align:center;padding-top:60px'>" +
                            "<h2>Connexion Google reussie</h2>" +
                            "<p>Vous pouvez fermer cette fenetre et retourner a SOSI Project.</p>" +
                            "</body></html>";

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes(StandardCharsets.UTF_8));
            }

            if (code != null) {
                codeQueue.offer(code);
            }
            if (returnedState != null) {
                stateQueue.offer(returnedState);
            }
        });

        server.start();

        try {
            String authUrl =
                    "https://accounts.google.com/o/oauth2/v2/auth" +
                            "?client_id=" + enc(AppConfig.GOOGLE_CLIENT_ID) +
                            "&redirect_uri=" + enc(redirectUri) +
                            "&response_type=code" +
                            "&scope=" + enc("openid email profile") +
                            "&state=" + enc(state) +
                            "&access_type=offline" +
                            "&prompt=select_account";

            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop browse non supporte. Ouvre manuellement : " + authUrl);
            }

            Desktop.getDesktop().browse(new URI(authUrl));

            String code = codeQueue.poll(120, TimeUnit.SECONDS);
            String returnedState = stateQueue.poll(5, TimeUnit.SECONDS);

            if (code == null) {
                throw new IllegalStateException("Connexion Google annulee ou expiree.");
            }

            if (!state.equals(returnedState)) {
                throw new IllegalStateException("State OAuth invalide.");
            }

            String accessToken = exchangeCodeForAccessToken(code, redirectUri);
            return fetchUserInfo(accessToken);

        } finally {
            server.stop(0);
        }
    }

    private String exchangeCodeForAccessToken(String code, String redirectUri) throws Exception {
        String body =
                "code=" + enc(code) +
                        "&client_id=" + enc(AppConfig.GOOGLE_CLIENT_ID) +
                        "&client_secret=" + enc(AppConfig.GOOGLE_CLIENT_SECRET) +
                        "&redirect_uri=" + enc(redirectUri) +
                        "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur token Google : " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getString("access_token");
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur profil Google : " + response.body());
        }

        JSONObject json = new JSONObject(response.body());

        return new GoogleUserInfo(
                json.optString("email", ""),
                json.optString("given_name", "Utilisateur"),
                json.optString("family_name", "Google")
        );
    }

    private void validateConfig() {
        if (AppConfig.GOOGLE_CLIENT_ID.startsWith("REMPLACE")
                || AppConfig.GOOGLE_CLIENT_SECRET.startsWith("REMPLACE")) {
            throw new IllegalStateException(
                    "Configure GOOGLE_CLIENT_ID et GOOGLE_CLIENT_SECRET dans AppConfig.java."
            );
        }
    }

    private String generateState() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getQueryParam(String query, String key) {
        if (query == null) {
            return null;
        }

        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);

            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}