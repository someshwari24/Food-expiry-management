package com.foodexpiry;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailService {

    private final String senderEmail;
    private final String senderPassword;
    private final String smtpHost;
    private final String smtpPort;

    public EmailService() {

        senderEmail = System.getenv("EMAIL_USERNAME");
        senderPassword = System.getenv("EMAIL_APP_PASSWORD");

        String configuredHost =
                System.getenv("EMAIL_SMTP_HOST");

        String configuredPort =
                System.getenv("EMAIL_SMTP_PORT");

        smtpHost =
                configuredHost == null
                        || configuredHost.isBlank()
                        ? "smtp.gmail.com"
                        : configuredHost;

        smtpPort =
                configuredPort == null
                        || configuredPort.isBlank()
                        ? "587"
                        : configuredPort;
    }

    public boolean isConfigured() {

        return senderEmail != null
                && !senderEmail.isBlank()
                && senderPassword != null
                && !senderPassword.isBlank();
    }

    public boolean sendExpiryNotification(
            String receiverEmail,
            String userName,
            String foodName,
            String expiryDate,
            long remainingDays
    ) {

        if (!isConfigured()) {

            System.err.println(
                    "Email configuration is missing. "
                            + "Set EMAIL_USERNAME and "
                            + "EMAIL_APP_PASSWORD."
            );

            return false;
        }

        if (receiverEmail == null
                || receiverEmail.isBlank()) {

            System.err.println(
                    "Receiver email is missing."
            );

            return false;
        }

        Properties properties =
                createMailProperties();

        Session session =
                Session.getInstance(
                        properties,
                        new Authenticator() {

                            @Override
                            protected PasswordAuthentication
                            getPasswordAuthentication() {

                                return new PasswordAuthentication(
                                        senderEmail,
                                        senderPassword
                                );
                            }
                        }
                );

        try {

            MimeMessage message =
                    new MimeMessage(session);

            message.setFrom(
                    new InternetAddress(
                            senderEmail,
                            "Food Expiry Management System",
                            StandardCharsets.UTF_8.name()
                    )
            );

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(receiverEmail)
            );

            message.setSubject(
                    "Food Expiry Reminder: " + foodName,
                    StandardCharsets.UTF_8.name()
            );

            String safeUserName =
                    userName == null
                            || userName.isBlank()
                            ? "User"
                            : userName;

            String htmlBody =
                    createHtmlBody(
                            safeUserName,
                            foodName,
                            expiryDate,
                            remainingDays
                    );

            message.setContent(
                    htmlBody,
                    "text/html; charset=UTF-8"
            );

            Transport.send(message);

            System.out.println(
                    "Expiry notification sent to "
                            + receiverEmail
                            + " for food: "
                            + foodName
            );

            return true;

        } catch (Exception exception) {

            System.err.println(
                    "Failed to send email to "
                            + receiverEmail
                            + ": "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            return false;
        }
    }

    private Properties createMailProperties() {

        Properties properties =
                new Properties();

        properties.put(
                "mail.smtp.auth",
                "true"
        );

        properties.put(
                "mail.smtp.starttls.enable",
                "true"
        );

        properties.put(
                "mail.smtp.starttls.required",
                "true"
        );

        properties.put(
                "mail.smtp.host",
                smtpHost
        );

        properties.put(
                "mail.smtp.port",
                smtpPort
        );

        properties.put(
                "mail.smtp.connectiontimeout",
                "10000"
        );

        properties.put(
                "mail.smtp.timeout",
                "10000"
        );

        properties.put(
                "mail.smtp.writetimeout",
                "10000"
        );

        return properties;
    }

    private String createHtmlBody(
            String userName,
            String foodName,
            String expiryDate,
            long remainingDays
    ) {

        String expiryMessage;

        if (remainingDays == 0) {

            expiryMessage =
                    "expires today";

        } else if (remainingDays == 1) {

            expiryMessage =
                    "will expire tomorrow";

        } else {

            expiryMessage =
                    "will expire in "
                            + remainingDays
                            + " days";
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="
                    margin:0;
                    padding:20px;
                    background:#f5f5f5;
                    font-family:Arial,sans-serif;
                ">
                    <div style="
                        max-width:600px;
                        margin:auto;
                        background:white;
                        padding:30px;
                        border-radius:10px;
                        box-shadow:0 2px 8px rgba(0,0,0,0.1);
                    ">
                        <h2 style="color:#2e7d32;">
                            Food Expiry Reminder
                        </h2>

                        <p>Hello %s,</p>

                        <p>
                            Your food item
                            <strong>%s</strong>
                            %s.
                        </p>

                        <div style="
                            background:#fff3cd;
                            padding:15px;
                            border-radius:6px;
                            margin:20px 0;
                        ">
                            <strong>Food item:</strong> %s
                            <br>
                            <strong>Expiry date:</strong> %s
                        </div>

                        <p>
                            Please consume, donate or safely
                            dispose of the item before it expires.
                        </p>

                        <p style="
                            color:#777;
                            margin-top:30px;
                            font-size:13px;
                        ">
                            Food Expiry Management System
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(userName),
                escapeHtml(foodName),
                expiryMessage,
                escapeHtml(foodName),
                escapeHtml(expiryDate)
        );
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}