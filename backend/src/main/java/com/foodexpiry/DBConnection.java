package com.foodexpiry;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;

public class DBConnection {

    private static final String DATABASE_NAME =
            "food_expiry_db";

    private static MongoClient mongoClient;

    private DBConnection() {
    }

    public static synchronized MongoDatabase getDatabase() {

        if (mongoClient == null) {

            String connectionUrl =
                    System.getenv("MONGODB_URI");

            /*
             * For local development, read backend/.env
             * when the operating-system variable is absent.
             */
            if (connectionUrl == null
                    || connectionUrl.isBlank()) {

                Dotenv dotenv = Dotenv.configure()
                        .ignoreIfMissing()
                        .load();

                connectionUrl =
                        dotenv.get("MONGODB_URI");
            }

            /*
             * Final local fallback.
             */
            if (connectionUrl == null
                    || connectionUrl.isBlank()) {

                connectionUrl =
                        "mongodb://localhost:27017";
            }

            mongoClient =
                    MongoClients.create(connectionUrl);

            System.out.println(
                    "MongoDB client initialized."
            );
        }

        return mongoClient.getDatabase(
                DATABASE_NAME
        );
    }

    public static synchronized void closeConnection() {

        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}