package com.foodexpiry;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ExpiryNotificationService {

    private static final int DEFAULT_REMINDER_DAYS = 3;

    private final MongoCollection<Document> foodCollection;
    private final MongoCollection<Document> userCollection;
    private final EmailService emailService;

    public ExpiryNotificationService(
            MongoDatabase database
    ) {

        // Must match the collection used in FoodHandler.
        foodCollection =
                database.getCollection("food_items");

        userCollection =
                database.getCollection("users");

        emailService =
                new EmailService();
    }

    public void checkAndSendNotifications() {

        System.out.println(
                "Running expiry notification check..."
        );

        if (!emailService.isConfigured()) {

            System.err.println(
                    "Email service is not configured. "
                            + "Notification check skipped."
            );

            return;
        }

        LocalDate today =
                LocalDate.now();

        int checkedCount = 0;
        int sentCount = 0;

        try {

            for (Document food
                    : foodCollection.find()) {

                checkedCount++;

                try {

                    boolean sent =
                            processFood(
                                    food,
                                    today
                            );

                    if (sent) {
                        sentCount++;
                    }

                } catch (Exception exception) {

                    System.err.println(
                            "Failed to process food document "
                                    + food.get("_id")
                                    + ": "
                                    + exception.getMessage()
                    );

                    exception.printStackTrace();
                }
            }

            System.out.println(
                    "Notification check completed. "
                            + "Checked: "
                            + checkedCount
                            + ", Sent: "
                            + sentCount
            );

        } catch (Exception exception) {

            System.err.println(
                    "Notification scheduler error: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private boolean processFood(
            Document food,
            LocalDate today
    ) {

        String expiryDateValue =
                readString(
                        food,
                        "expiryDate"
                );

        if (expiryDateValue == null
                || expiryDateValue.isBlank()) {

            System.err.println(
                    "Expiry date missing for food: "
                            + food.get("_id")
            );

            return false;
        }

        LocalDate expiryDate;

        try {

            expiryDate =
                    LocalDate.parse(
                            expiryDateValue
                    );

        } catch (DateTimeParseException exception) {

            System.err.println(
                    "Invalid expiry date format for food "
                            + food.get("_id")
                            + ": "
                            + expiryDateValue
            );

            return false;
        }

        int reminderDays =
                getReminderDays(food);

        long remainingDays =
                ChronoUnit.DAYS.between(
                        today,
                        expiryDate
                );

        String itemName =
                firstNonBlank(
                        readString(food, "itemName"),
                        readString(food, "foodName"),
                        readString(food, "name"),
                        "Food item"
                );

        System.out.println(
                "Checking food: " + itemName
        );

        System.out.println(
                "Today: " + today
        );

        System.out.println(
                "Expiry date: " + expiryDate
        );

        System.out.println(
                "Remaining days: " + remainingDays
        );

        System.out.println(
                "Reminder days: " + reminderDays
        );

        /*
         * Example:
         *
         * remainingDays = 0
         * reminderDays = 0
         *
         * Email is sent on the expiry date.
         */
        if (remainingDays != reminderDays) {

            System.out.println(
                    "Reminder not due for: "
                            + itemName
            );

            return false;
        }

        if (wasNotificationSentToday(
                food,
                today,
                reminderDays
        )) {

            System.out.println(
                    "Notification already sent today for: "
                            + itemName
            );

            return false;
        }

        String userId =
                readString(
                        food,
                        "userId"
                );

        if (userId == null
                || userId.isBlank()) {

            System.err.println(
                    "Food document does not contain userId: "
                            + food.get("_id")
            );

            return false;
        }

        Document user =
                findUserById(userId);

        if (user == null) {

            System.err.println(
                    "User not found for userId: "
                            + userId
            );

            return false;
        }

        String receiverEmail =
                readString(
                        user,
                        "email"
                );

        String userName =
                firstNonBlank(
                        readString(user, "name"),
                        readString(user, "username"),
                        "User"
                );

        if (receiverEmail == null
                || receiverEmail.isBlank()) {

            System.err.println(
                    "User does not have an email: "
                            + userId
            );

            return false;
        }

        System.out.println(
                "Reminder matched for: "
                        + itemName
        );

        System.out.println(
                "Sending email to: "
                        + receiverEmail
        );

        boolean emailSent =
                emailService.sendExpiryNotification(
                        receiverEmail,
                        userName,
                        itemName,
                        expiryDate.toString(),
                        remainingDays
                );

        if (emailSent) {

            markNotificationAsSent(
                    food,
                    today,
                    reminderDays
            );

            System.out.println(
                    "Expiry notification sent successfully "
                            + "for: "
                            + itemName
            );

            return true;
        }

        System.err.println(
                "Email sending failed for: "
                        + itemName
        );

        return false;
    }

    private int getReminderDays(
            Document food
    ) {

        Object value =
                food.get("reminderDays");

        if (value == null) {

            return DEFAULT_REMINDER_DAYS;
        }

        if (value instanceof Number number) {

            int reminderDays =
                    number.intValue();

            if (reminderDays < 0
                    || reminderDays > 30) {

                return DEFAULT_REMINDER_DAYS;
            }

            return reminderDays;
        }

        try {

            int reminderDays =
                    Integer.parseInt(
                            value.toString().trim()
                    );

            if (reminderDays < 0
                    || reminderDays > 30) {

                return DEFAULT_REMINDER_DAYS;
            }

            return reminderDays;

        } catch (NumberFormatException exception) {

            return DEFAULT_REMINDER_DAYS;
        }
    }

    private Document findUserById(
            String userId
    ) {

        List<Bson> filters =
                new ArrayList<>();

        filters.add(
                Filters.eq(
                        "id",
                        userId
                )
        );

        filters.add(
                Filters.eq(
                        "userId",
                        userId
                )
        );

        filters.add(
                Filters.eq(
                        "_id",
                        userId
                )
        );

        if (ObjectId.isValid(userId)) {

            filters.add(
                    Filters.eq(
                            "_id",
                            new ObjectId(userId)
                    )
            );
        }

        return userCollection
                .find(
                        Filters.or(filters)
                )
                .first();
    }

    private boolean wasNotificationSentToday(
            Document food,
            LocalDate today,
            int reminderDays
    ) {

        String sentDate =
                readString(
                        food,
                        "notificationSentDate"
                );

        Integer previousReminderDays =
                readIntegerSafely(
                        food,
                        "notificationReminderDays"
                );

        return today.toString().equals(sentDate)
                && previousReminderDays != null
                && previousReminderDays
                == reminderDays;
    }

    private void markNotificationAsSent(
            Document food,
            LocalDate today,
            int reminderDays
    ) {

        Object mongoId =
                food.get("_id");

        if (mongoId == null) {

            System.err.println(
                    "Cannot update notification status: "
                            + "food _id is missing."
            );

            return;
        }

        foodCollection.updateOne(
                Filters.eq(
                        "_id",
                        mongoId
                ),
                Updates.combine(
                        Updates.set(
                                "notificationSent",
                                true
                        ),
                        Updates.set(
                                "notificationSentDate",
                                today.toString()
                        ),
                        Updates.set(
                                "notificationReminderDays",
                                reminderDays
                        )
                )
        );
    }

    private Integer readIntegerSafely(
            Document document,
            String field
    ) {

        Object value =
                document.get(field);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {

            return Integer.parseInt(
                    value.toString().trim()
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    private String readString(
            Document document,
            String field
    ) {

        Object value =
                document.get(field);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private String firstNonBlank(
            String... values
    ) {

        for (String value : values) {

            if (value != null
                    && !value.isBlank()) {

                return value;
            }
        }

        return null;
    }
}