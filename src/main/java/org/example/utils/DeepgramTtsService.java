package org.example.utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DeepgramTtsService {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String API_KEY = dotenv.get("DEEPGRAM_API_KEY");
    private static final String API_URL = "https://api.deepgram.com/v1/speak?model=aura-luna-en";

    private final HttpClient client;

    public DeepgramTtsService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public InputStream generateSpeech(String text) throws Exception {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("Deepgram API Key is missing in .env");
        }

        int maxRetries = 2;
        int attempt = 0;

        while (attempt <= maxRetries) {
            try {
                JSONObject body = new JSONObject();
                body.put("text", text);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Token " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    throw new Exception("Deepgram TTS Error: " + response.statusCode());
                }
            } catch (java.net.http.HttpConnectTimeoutException | java.net.ConnectException e) {
                attempt++;
                if (attempt > maxRetries) throw e;
                System.err.println("Deepgram TTS connection timeout, retrying... (Attempt " + attempt + ")");
                Thread.sleep(1000 * attempt);
            }
        }
        throw new Exception("Failed to generate speech after retries");
    }
}
