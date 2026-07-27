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

        senderEmail =
                getEnvironmentValue(
                        "EMAIL_USERNAME"
                );

        senderPassword =
                getEnvironmentValue(
                        "EMAIL_APP_PASSWORD"
                );

        String configuredHost =
                getEnvironmentValue(
                        "EMAIL_SMTP_HOST"
                );

        String configuredPort =
                getEnvironmentValue(
                        "EMAIL_SMTP_PORT"
                );

        smtpHost =
                configuredHost == null
                        ? "smtp.gmail.com"
                        : configuredHost;

        smtpPort =
                configuredPort == null
                        ? "587"
                        : configuredPort;

        System.out.println(
                "Email username configured: "
                        + (senderEmail != null)
        );

        System.out.println(
                "Email password configured: "
                        + (senderPassword != null)
        );

        System.out.println(
                "SMTP configuration: "
                        + smtpHost
                        + ":"
                        + smtpPort
        );
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

        receiverEmail =
                receiverEmail.trim();

        try {

            InternetAddress receiverAddress =
                    new InternetAddress(
                            receiverEmail
                    );

            receiverAddress.validate();

        } catch (Exception exception) {

            System.err.println(
                    "Invalid receiver email address: "
                            + receiverEmail
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

        /*
         * Temporarily keep this true while testing.
         * It prints SMTP communication in Render logs.
         */
        session.setDebug(false);

        try {

            String safeFoodName =
                    foodName == null
                            || foodName.isBlank()
                            ? "Food item"
                            : foodName.trim();

            String safeUserName =
                    userName == null
                            || userName.isBlank()
                            ? "User"
                            : userName.trim();

            String safeExpiryDate =
                    expiryDate == null
                            || expiryDate.isBlank()
                            ? "Not available"
                            : expiryDate.trim();

            MimeMessage message =
                    new MimeMessage(session);

            InternetAddress senderAddress =
                    new InternetAddress(
                            senderEmail,
                            "Food Expiry Management System",
                            StandardCharsets.UTF_8.name()
                    );

            message.setFrom(senderAddress);

            message.setRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(
                            receiverEmail
                    )
            );

            message.setSubject(
                    "Food Expiry Reminder: "
                            + safeFoodName,
                    StandardCharsets.UTF_8.name()
            );

            String htmlBody =
                    createHtmlBody(
                            safeUserName,
                            safeFoodName,
                            safeExpiryDate,
                            remainingDays
                    );

            message.setContent(
                    htmlBody,
                    "text/html; charset=UTF-8"
            );

            message.saveChanges();

            System.out.println(
                    "Attempting to send expiry email to: "
                            + receiverEmail
            );

            Transport.send(message);

            System.out.println(
                    "Expiry notification sent successfully to "
                            + receiverEmail
                            + " for food: "
                            + safeFoodName
            );

            return true;

        } catch (Exception exception) {

            System.err.println(
                    "Failed to send email to "
                            + receiverEmail
                            + ". Error type: "
                            + exception.getClass()
                            .getSimpleName()
                            + ". Message: "
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
                "mail.smtp.ssl.trust",
                smtpHost
        );

        properties.put(
                "mail.smtp.ssl.protocols",
                "TLSv1.2"
        );

        properties.put(
                "mail.smtp.connectiontimeout",
                "20000"
        );

        properties.put(
                "mail.smtp.timeout",
                "20000"
        );

        properties.put(
                "mail.smtp.writetimeout",
                "20000"
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

        if (remainingDays < 0) {

            expiryMessage =
                    "has already expired";

        } else if (remainingDays == 0) {

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
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                </head>

                <body style="
                    margin:0;
                    padding:20px;
                    background:#f5f5f5;
                    font-family:Arial,sans-serif;
                    color:#333333;
                ">

                    <div style="
                        max-width:600px;
                        margin:0 auto;
                        background:#ffffff;
                        padding:30px;
                        border-radius:10px;
                        box-shadow:0 2px 8px rgba(0,0,0,0.1);
                    ">

                        <h2 style="
                            color:#2e7d32;
                            margin-top:0;
                        ">
                            Food Expiry Reminder
                        </h2>

                        <p>
                            Hello %s,
                        </p>

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
                            border-left:5px solid #ffc107;
                        ">

                            <strong>Food item:</strong>
                            %s

                            <br><br>

                            <strong>Expiry date:</strong>
                            %s

                        </div>

                        <p>
                            Please consume, donate, or safely
                            dispose of the item before it expires.
                        </p>

                        <p style="
                            color:#777777;
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
                escapeHtml(expiryMessage),
                escapeHtml(foodName),
                escapeHtml(expiryDate)
        );
    }

    private String getEnvironmentValue(
            String variableName
    ) {

        String value =
                System.getenv(
                        variableName
                );

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private String escapeHtml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "&",
                        "&amp;"
                )
                .replace(
                        "<",
                        "&lt;"
                )
                .replace(
                        ">",
                        "&gt;"
                )
                .replace(
                        "\"",
                        "&quot;"
                )
                .replace(
                        "'",
                        "&#39;"
                );
    }
}