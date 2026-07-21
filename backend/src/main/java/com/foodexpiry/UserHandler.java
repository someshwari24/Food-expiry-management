package com.foodexpiry;

import com.mongodb.client.MongoCollection;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

public class UserHandler implements HttpHandler {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final String operation;

    public UserHandler(String operation) {
        this.operation = operation;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (CorsUtil.handleOptions(exchange)) {
            return;
        }

        try {
            switch (operation) {
                case "register" -> register(exchange);
                case "login" -> login(exchange);

                default -> ResponseUtil.sendMessage(
                        exchange,
                        404,
                        "error",
                        "Invalid user operation"
                );
            }

        } catch (IllegalArgumentException exception) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    exception.getMessage()
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            ResponseUtil.sendMessage(
                    exchange,
                    500,
                    "error",
                    "Internal server error"
            );
        }
    }

    private MongoCollection<Document> getUsersCollection() {
        return DBConnection.getDatabase()
                .getCollection("users");
    }

    private void register(HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {
            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only POST method is allowed"
            );
            return;
        }

        Document input = RequestUtil.readJson(exchange);

        String name = clean(input.getString("name"));
        String email = normalizeEmail(
                input.getString("email")
        );
        String password = input.getString("password");

        validateRegistration(name, email, password);

        MongoCollection<Document> users =
                getUsersCollection();

        Document existingUser = users.find(
                new Document("email", email)
        ).first();

        if (existingUser != null) {
            ResponseUtil.sendMessage(
                    exchange,
                    409,
                    "error",
                    "User already exists"
            );
            return;
        }

        String hashedPassword =
                BCrypt.hashpw(
                        password,
                        BCrypt.gensalt(12)
                );

        Document user = new Document()
                .append("name", name)
                .append("email", email)
                .append("password", hashedPassword);

        users.insertOne(user);

        ResponseUtil.sendMessage(
                exchange,
                201,
                "success",
                "Registration successful"
        );
    }

    private void login(HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {
            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only POST method is allowed"
            );
            return;
        }

        Document input = RequestUtil.readJson(exchange);

        String email = normalizeEmail(
                input.getString("email")
        );
        String password = input.getString("password");

        if (isBlank(email) || isBlank(password)) {
            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "Email and password are required"
            );
            return;
        }

        MongoCollection<Document> users =
                getUsersCollection();

        Document user = users.find(
                new Document("email", email)
        ).first();

        if (user == null) {
            sendInvalidCredentials(exchange);
            return;
        }

        String storedPassword =
                user.getString("password");

        boolean passwordMatches;

        try {
            passwordMatches =
                    storedPassword != null
                            && BCrypt.checkpw(
                                    password,
                                    storedPassword
                            );

        } catch (IllegalArgumentException exception) {
            passwordMatches = false;
        }

        if (!passwordMatches) {
            sendInvalidCredentials(exchange);
            return;
        }

        Document response = new Document()
                .append("status", "success")
                .append("message", "Login successful")
                .append(
                        "userId",
                        user.getObjectId("_id")
                                .toHexString()
                )
                .append(
                        "name",
                        user.getString("name")
                )
                .append(
                        "email",
                        user.getString("email")
                );

        ResponseUtil.sendJson(
                exchange,
                200,
                response.toJson()
        );
    }

    private void validateRegistration(
            String name,
            String email,
            String password
    ) {

        if (isBlank(name)
                || isBlank(email)
                || isBlank(password)) {
            throw new IllegalArgumentException(
                    "Name, email and password are required"
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(
                    "Enter a valid email address"
            );
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }
    }

    private void sendInvalidCredentials(
            HttpExchange exchange
    ) throws IOException {

        ResponseUtil.sendMessage(
                exchange,
                401,
                "error",
                "Invalid email or password"
        );
    }

    private String normalizeEmail(String email) {

        if (email == null) {
            return null;
        }

        return email.trim()
                .toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}