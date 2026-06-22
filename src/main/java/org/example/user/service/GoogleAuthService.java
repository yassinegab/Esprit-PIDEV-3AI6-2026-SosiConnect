package org.example.user.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import io.github.cdimascio.dotenv.Dotenv;

public class GoogleAuthService {

    private static String CLIENT_ID;
    private static String CLIENT_SECRET;

    static {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID") != null ? dotenv.get("GOOGLE_CLIENT_ID") : System.getenv("GOOGLE_CLIENT_ID");
            CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET") != null ? dotenv.get("GOOGLE_CLIENT_SECRET") : System.getenv("GOOGLE_CLIENT_SECRET");
        } catch (Exception e) {
            System.err.println("Warning: Could not load .env file safely. " + e.getMessage());
            CLIENT_ID = System.getenv("GOOGLE_CLIENT_ID");
            CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET");
        }
    }
    
    private static final List<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            CalendarScopes.CALENDAR
    );

    /**
     * Ouvre le navigateur, authentifie l'utilisateur via Google et retourne ses informations
     * sous forme de JSONObject contenant "email", "given_name" (prénom), et "family_name" (nom).
     */
    public static JSONObject authenticateAndGetUserInfo() throws Exception {
        // 1. Configurer le flux OAuth
        String clientSecretsJson = "{\"installed\":{\"client_id\":\"" + CLIENT_ID + "\",\"client_secret\":\"" + CLIENT_SECRET + "\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\"}}";
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), 
                new StringReader(clientSecretsJson)
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(),
                clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        // 2. Démarrer le serveur local pour intercepter le code de retour sur un port dynamique
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(-1).build();
        
        AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flow, receiver) {
            @Override
            protected void onAuthorization(com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl authorizationUrl) throws java.io.IOException {
                String url = authorizationUrl.build();
                org.example.main.MainFX.getInstance().openUrl(url);
            }
        };
        
        Credential credential = app.authorize("user");

        // 3. Obtenir le token d'accès
        String accessToken = credential.getAccessToken();

        // 4. Faire une requête à l'API Google pour obtenir les informations de l'utilisateur
        return fetchUserInfo(accessToken);
    }

    private static JSONObject fetchUserInfo(String accessToken) throws Exception {
        URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();
            return new JSONObject(response.toString());
        } else {
            throw new RuntimeException("Failed to fetch user info: HTTP " + responseCode);
        }
    }

    /**
     * Retourne une instance authentifiée du service Google Calendar.
     */
    public static Calendar getCalendarService() throws Exception {
        String clientSecretsJson = "{\"installed\":{\"client_id\":\"" + CLIENT_ID + "\",\"client_secret\":\"" + CLIENT_SECRET + "\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\"}}";
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), 
                new StringReader(clientSecretsJson)
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(),
                clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(-1).build();
        
        AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flow, receiver) {
            @Override
            protected void onAuthorization(com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl authorizationUrl) throws java.io.IOException {
                String url = authorizationUrl.build();
                org.example.main.MainFX.getInstance().openUrl(url);
            }
        };
        
        Credential credential = app.authorize("user");

        return new Calendar.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("SosiProject")
                .build();
    }
}
