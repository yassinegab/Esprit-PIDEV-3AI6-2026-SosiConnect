package org.example.user.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javafx.scene.control.TextInputDialog;
import org.example.config.AppConfig;
import org.example.utils.AlertUtil;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Properties;

public class EmailVerificationService {

    private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    public void sendVerificationCode(String toEmail, String code) throws MessagingException {
        if (AppConfig.EMAIL_DEVELOPMENT_MODE) {
            AlertUtil.showInfo("Code verification DEV", "Code pour " + toEmail + " : " + code);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.SMTP_HOST);
        props.put("mail.smtp.port", AppConfig.SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", AppConfig.SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        AppConfig.SMTP_USERNAME,
                        AppConfig.SMTP_APP_PASSWORD.replace(" ", "")
                );
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(AppConfig.SMTP_USERNAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Code de verification SOSI Project");

        message.setText(
                "Bonjour,\n\n" +
                        "Votre code de verification SOSI Project est : " + code + "\n\n" +
                        "Veuillez saisir ce code pour finaliser votre inscription.\n\n" +
                        "SOSI Project"
        );

        Transport.send(message);
    }

    public boolean askCodeAndValidate(String expectedCode) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Verification email");
        dialog.setHeaderText("Un code de verification a ete envoye par email.");
        dialog.setContentText("Entrez le code recu :");

        Optional<String> result = dialog.showAndWait();
        return result.isPresent() && expectedCode.equals(result.get().trim());
    }
}