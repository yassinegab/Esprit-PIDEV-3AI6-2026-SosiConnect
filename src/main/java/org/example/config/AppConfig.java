package org.example.config;

public final class AppConfig {
    private AppConfig() {}

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Variable d'environnement manquante: " + key);
        }
        return value;
    }

    public static final String GOOGLE_CLIENT_ID = getEnv("GOOGLE_CLIENT_ID");
    public static final String GOOGLE_CLIENT_SECRET = getEnv("GOOGLE_CLIENT_SECRET");
    public static final int GOOGLE_REDIRECT_PORT = 8888;

    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
    public static final String SMTP_USERNAME = getEnv("SMTP_USERNAME");
    public static final String SMTP_APP_PASSWORD = getEnv("SMTP_APP_PASSWORD");
    public static final String SMTP_FROM_NAME = "SOSI Project";

    public static final boolean EMAIL_DEVELOPMENT_MODE = false;
    public static final String RECAPTCHA_SITE_KEY = getEnv("RECAPTCHA_SITE_KEY");
    public static final String RECAPTCHA_SECRET_KEY = getEnv("RECAPTCHA_SECRET_KEY");
}