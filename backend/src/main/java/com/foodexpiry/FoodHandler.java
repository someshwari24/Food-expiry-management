package com.foodexpiry;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class FoodHandler implements HttpHandler {

    private final String operation;

    public FoodHandler(String operation) {
        this.operation = operation;
    }

    @Override
    public void handle(
            HttpExchange exchange
    ) throws IOException {

        if (CorsUtil.handleOptions(exchange)) {
            return;
        }

        try {

            switch (operation) {

                case "add" ->
                        addFood(exchange);

                case "view" ->
                        viewFood(exchange);

                case "update" ->
                        updateFood(exchange);

                case "delete" ->
                        deleteFood(exchange);

                case "search" ->
                        searchFood(exchange);

                case "expiringSoon" ->
                        viewExpiringSoon(exchange);

                case "expired" ->
                        viewExpired(exchange);

                default ->
                        ResponseUtil.sendMessage(
                                exchange,
                                404,
                                "error",
                                "Invalid food operation"
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

    private MongoCollection<Document> getCollection() {

        return DBConnection
                .getDatabase()
                .getCollection("food_items");
    }

    private void addFood(
            HttpExchange exchange
    ) throws IOException {

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

        Document input =
                RequestUtil.readJson(exchange);

        String userId =
                input.getString("userId");

        String itemName =
                input.getString("itemName");

        String category =
                input.getString("category");

        Integer quantity =
                readInteger(
                        input,
                        "quantity",
                        null
                );

        String purchaseDate =
                input.getString("purchaseDate");

        String expiryDate =
                input.getString("expiryDate");

        Integer reminderDays =
                readInteger(
                        input,
                        "reminderDays",
                        3
                );

        validateFood(
                userId,
                itemName,
                category,
                quantity,
                purchaseDate,
                expiryDate,
                reminderDays
        );

        Document food =
                new Document()
                        .append(
                                "userId",
                                userId.trim()
                        )
                        .append(
                                "itemName",
                                itemName.trim()
                        )
                        .append(
                                "category",
                                category.trim()
                        )
                        .append(
                                "quantity",
                                quantity
                        )
                        .append(
                                "purchaseDate",
                                purchaseDate
                        )
                        .append(
                                "expiryDate",
                                expiryDate
                        )
                        .append(
                                "reminderDays",
                                reminderDays
                        )
                        .append(
                                "notificationSent",
                                false
                        )
                        .append(
                                "createdAt",
                                LocalDate.now().toString()
                        );

        getCollection().insertOne(food);

        Document response =
                new Document()
                        .append(
                                "status",
                                "success"
                        )
                        .append(
                                "message",
                                "Food item added successfully"
                        )
                        .append(
                                "id",
                                food.getObjectId("_id")
                                        .toHexString()
                        );

        ResponseUtil.sendJson(
                exchange,
                201,
                response.toJson()
        );
    }

    private void viewFood(
            HttpExchange exchange
    ) throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only GET method is allowed"
            );

            return;
        }

        Map<String, String> parameters =
                RequestUtil.getQueryParameters(
                        exchange
                );

        String userId =
                parameters.get("userId");

        if (isBlank(userId)) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "userId is required"
            );

            return;
        }

        FindIterable<Document> foods =
                getCollection().find(
                        Filters.eq(
                                "userId",
                                userId.trim()
                        )
                );

        ResponseUtil.sendJson(
                exchange,
                200,
                documentsToJson(foods)
        );
    }

    private void updateFood(
            HttpExchange exchange
    ) throws IOException {

        if (!"PUT".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only PUT method is allowed"
            );

            return;
        }

        Document input =
                RequestUtil.readJson(exchange);

        String id =
                input.getString("id");

        String userId =
                input.getString("userId");

        String itemName =
                input.getString("itemName");

        String category =
                input.getString("category");

        Integer quantity =
                readInteger(
                        input,
                        "quantity",
                        null
                );

        String purchaseDate =
                input.getString("purchaseDate");

        String expiryDate =
                input.getString("expiryDate");

        Integer reminderDays =
                readInteger(
                        input,
                        "reminderDays",
                        3
                );

        if (isBlank(id)
                || !ObjectId.isValid(id)) {

            throw new IllegalArgumentException(
                    "Invalid food item ID"
            );
        }

        validateFood(
                userId,
                itemName,
                category,
                quantity,
                purchaseDate,
                expiryDate,
                reminderDays
        );

        Bson filter =
                Filters.and(
                        Filters.eq(
                                "_id",
                                new ObjectId(id)
                        ),
                        Filters.eq(
                                "userId",
                                userId.trim()
                        )
                );

        Bson updates =
                Updates.combine(
                        Updates.set(
                                "itemName",
                                itemName.trim()
                        ),
                        Updates.set(
                                "category",
                                category.trim()
                        ),
                        Updates.set(
                                "quantity",
                                quantity
                        ),
                        Updates.set(
                                "purchaseDate",
                                purchaseDate
                        ),
                        Updates.set(
                                "expiryDate",
                                expiryDate
                        ),
                        Updates.set(
                                "reminderDays",
                                reminderDays
                        ),
                        Updates.set(
                                "notificationSent",
                                false
                        ),
                        Updates.unset(
                                "notificationSentDate"
                        ),
                        Updates.unset(
                                "notificationReminderDays"
                        )
                );

        UpdateResult result =
                getCollection().updateOne(
                        filter,
                        updates
                );

        if (result.getMatchedCount() == 0) {

            ResponseUtil.sendMessage(
                    exchange,
                    404,
                    "error",
                    "Food item not found"
            );

            return;
        }

        ResponseUtil.sendMessage(
                exchange,
                200,
                "success",
                "Food item updated successfully"
        );
    }

    private void deleteFood(
            HttpExchange exchange
    ) throws IOException {

        if (!"DELETE".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only DELETE method is allowed"
            );

            return;
        }

        Document input =
                RequestUtil.readJson(exchange);

        String id =
                input.getString("id");

        String userId =
                input.getString("userId");

        if (isBlank(id)
                || !ObjectId.isValid(id)) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "Invalid food item ID"
            );

            return;
        }

        if (isBlank(userId)) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "userId is required"
            );

            return;
        }

        DeleteResult result =
                getCollection().deleteOne(
                        Filters.and(
                                Filters.eq(
                                        "_id",
                                        new ObjectId(id)
                                ),
                                Filters.eq(
                                        "userId",
                                        userId.trim()
                                )
                        )
                );

        if (result.getDeletedCount() == 0) {

            ResponseUtil.sendMessage(
                    exchange,
                    404,
                    "error",
                    "Food item not found"
            );

            return;
        }

        ResponseUtil.sendMessage(
                exchange,
                200,
                "success",
                "Food item deleted successfully"
        );
    }

    private void searchFood(
            HttpExchange exchange
    ) throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only GET method is allowed"
            );

            return;
        }

        Map<String, String> parameters =
                RequestUtil.getQueryParameters(
                        exchange
                );

        String userId =
                parameters.get("userId");

        String itemName =
                parameters.get("itemName");

        String category =
                parameters.get("category");

        if (isBlank(userId)) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "userId is required"
            );

            return;
        }

        Bson filter =
                Filters.eq(
                        "userId",
                        userId.trim()
                );

        if (!isBlank(itemName)) {

            filter =
                    Filters.and(
                            filter,
                            Filters.regex(
                                    "itemName",
                                    itemName.trim(),
                                    "i"
                            )
                    );
        }

        if (!isBlank(category)) {

            filter =
                    Filters.and(
                            filter,
                            Filters.regex(
                                    "category",
                                    "^"
                                            + category.trim()
                                            + "$",
                                    "i"
                            )
                    );
        }

        FindIterable<Document> foods =
                getCollection().find(filter);

        ResponseUtil.sendJson(
                exchange,
                200,
                documentsToJson(foods)
        );
    }

    private void viewExpiringSoon(
            HttpExchange exchange
    ) throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only GET method is allowed"
            );

            return;
        }

        Map<String, String> parameters =
                RequestUtil.getQueryParameters(
                        exchange
                );

        String userId =
                parameters.get("userId");

        if (isBlank(userId)) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "userId is required"
            );

            return;
        }

        int days = 7;

        if (parameters.containsKey("days")
                && !isBlank(parameters.get("days"))) {

            try {

                days =
                        Integer.parseInt(
                                parameters.get("days")
                        );

            } catch (NumberFormatException exception) {

                throw new IllegalArgumentException(
                        "Days must be a valid number"
                );
            }
        }

        if (days < 0 || days > 365) {

            throw new IllegalArgumentException(
                    "Days must be between 0 and 365"
            );
        }

        LocalDate today =
                LocalDate.now();

        LocalDate lastDate =
                today.plusDays(days);

        FindIterable<Document> foods =
                getCollection().find(
                        Filters.eq(
                                "userId",
                                userId.trim()
                        )
                );

        StringBuilder json =
                new StringBuilder("[");

        boolean first = true;

        for (Document food : foods) {

            String expiryDateValue =
                    food.getString("expiryDate");

            if (isBlank(expiryDateValue)) {
                continue;
            }

            try {

                LocalDate expiryDate =
                        LocalDate.parse(
                                expiryDateValue
                        );

                boolean isNotExpired =
                        !expiryDate.isBefore(today);

                boolean isWithinRange =
                        !expiryDate.isAfter(lastDate);

                if (isNotExpired
                        && isWithinRange) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append(
                            food.toJson()
                    );

                    first = false;
                }

            } catch (DateTimeParseException exception) {

                System.err.println(
                        "Invalid expiry date for food: "
                                + food.get("_id")
                );
            }
        }

        json.append("]");

        ResponseUtil.sendJson(
                exchange,
                200,
                json.toString()
        );
    }

    private void viewExpired(
            HttpExchange exchange
    ) throws IOException {

        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            ResponseUtil.sendMessage(
                    exchange,
                    405,
                    "error",
                    "Only GET method is allowed"
            );

            return;
        }

        Map<String, String> parameters =
                RequestUtil.getQueryParameters(
                        exchange
                );

        String userId =
                parameters.get("userId");

        if (isBlank(userId)) {

            ResponseUtil.sendMessage(
                    exchange,
                    400,
                    "error",
                    "userId is required"
            );

            return;
        }

        LocalDate today =
                LocalDate.now();

        FindIterable<Document> foods =
                getCollection().find(
                        Filters.eq(
                                "userId",
                                userId.trim()
                        )
                );

        StringBuilder json =
                new StringBuilder("[");

        boolean first = true;

        for (Document food : foods) {

            String expiryDateValue =
                    food.getString("expiryDate");

            if (isBlank(expiryDateValue)) {
                continue;
            }

            try {

                LocalDate expiryDate =
                        LocalDate.parse(
                                expiryDateValue
                        );

                if (expiryDate.isBefore(today)) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append(
                            food.toJson()
                    );

                    first = false;
                }

            } catch (DateTimeParseException exception) {

                System.err.println(
                        "Invalid expiry date for food: "
                                + food.get("_id")
                );
            }
        }

        json.append("]");

        ResponseUtil.sendJson(
                exchange,
                200,
                json.toString()
        );
    }

    private String documentsToJson(
            FindIterable<Document> documents
    ) {

        StringBuilder json =
                new StringBuilder("[");

        boolean first = true;

        for (Document document : documents) {

            if (!first) {
                json.append(",");
            }

            json.append(
                    document.toJson()
            );

            first = false;
        }

        json.append("]");

        return json.toString();
    }

    private void validateFood(
            String userId,
            String itemName,
            String category,
            Integer quantity,
            String purchaseDate,
            String expiryDate,
            Integer reminderDays
    ) {

        if (isBlank(userId)
                || isBlank(itemName)
                || isBlank(category)
                || quantity == null
                || isBlank(purchaseDate)
                || isBlank(expiryDate)
                || reminderDays == null) {

            throw new IllegalArgumentException(
                    "All food fields are required"
            );
        }

        if (quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        if (reminderDays < 0
                || reminderDays > 30) {

            throw new IllegalArgumentException(
                    "Reminder days must be between 0 and 30"
            );
        }

        try {

            LocalDate purchase =
                    LocalDate.parse(
                            purchaseDate
                    );

            LocalDate expiry =
                    LocalDate.parse(
                            expiryDate
                    );

            if (expiry.isBefore(purchase)) {

                throw new IllegalArgumentException(
                        "Expiry date cannot be before purchase date"
                );
            }

            long totalDays =
                    ChronoUnit.DAYS.between(
                            purchase,
                            expiry
                    );

            if (reminderDays >= totalDays) {

                throw new IllegalArgumentException(
                        "Reminder days cannot be greater "
                                + "than the days between "
                                + "purchase and expiry"
                );
            }

        } catch (DateTimeParseException exception) {

            throw new IllegalArgumentException(
                    "Date format must be yyyy-MM-dd"
            );
        }
    }

    private Integer readInteger(
            Document document,
            String field,
            Integer defaultValue
    ) {

        Object value =
                document.get(field);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {

            return Integer.parseInt(
                    value.toString()
                            .trim()
            );

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    field
                            + " must be a valid number"
            );
        }
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }
}