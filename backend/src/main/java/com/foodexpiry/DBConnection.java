package com.foodexpiry;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DBConnection {

    private static final String DATABASE_NAME = "food_expiry_db";

    private static MongoClient mongoClient;

    private DBConnection() {
    }

    public static MongoDatabase getDatabase() {

        if (mongoClient == null) {

            String connectionUrl = System.getenv("MONGODB_URI");

            // Local development fallback
            if (connectionUrl == null || connectionUrl.isBlank()) {
                connectionUrl = "mongodb://localhost:27017";
            }

            mongoClient = MongoClients.create(connectionUrl);

            System.out.println("Connected to MongoDB");
        }

        return mongoClient.getDatabase(DATABASE_NAME);
    }

    public static void closeConnection() {

        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}