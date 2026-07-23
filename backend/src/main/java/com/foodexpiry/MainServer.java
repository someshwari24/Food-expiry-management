package com.foodexpiry;

import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bson.Document;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainServer {

    public static void main(String[] args) {

        ScheduledExecutorService scheduler =
                Executors
                        .newSingleThreadScheduledExecutor();

        try {

            int port = getPort();

            MongoDatabase database =
                    DBConnection.getDatabase();

            database.runCommand(
                    new Document("ping", 1)
            );

            ExpiryNotificationService
                    notificationService =
                    new ExpiryNotificationService(
                            database
                    );

            /*
             * Production:
             * Check immediately when the server starts,
             * then check every 24 hours.
             */
            scheduler.scheduleAtFixedRate(
                    notificationService
                            ::checkAndSendNotifications,
                    0,
                    24,
                    TimeUnit.HOURS
            );

            /*
             * For testing every minute,
             * temporarily replace the above scheduler with:
             *
             * scheduler.scheduleAtFixedRate(
             *     notificationService
             *         ::checkAndSendNotifications,
             *     0,
             *     1,
             *     TimeUnit.MINUTES
             * );
             */

            HttpServer server =
                    HttpServer.create(
                            new InetSocketAddress(
                                    "0.0.0.0",
                                    port
                            ),
                            0
                    );

            server.createContext(
                    "/api/register",
                    new UserHandler("register")
            );

            server.createContext(
                    "/api/login",
                    new UserHandler("login")
            );

            server.createContext(
                    "/api/foods/add",
                    new FoodHandler("add")
            );

            server.createContext(
                    "/api/foods/update",
                    new FoodHandler("update")
            );

            server.createContext(
                    "/api/foods/delete",
                    new FoodHandler("delete")
            );

            server.createContext(
                    "/api/foods/search",
                    new FoodHandler("search")
            );

            server.createContext(
                    "/api/foods/expiring-soon",
                    new FoodHandler(
                            "expiringSoon"
                    )
            );

            server.createContext(
                    "/api/foods/expired",
                    new FoodHandler("expired")
            );

            server.createContext(
                    "/api/foods",
                    new FoodHandler("view")
            );

            server.createContext(
                    "/",
                    MainServer::handleHealthCheck
            );

            server.setExecutor(
                    Executors.newFixedThreadPool(10)
            );

            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(() -> {

                                System.out.println(
                                        "Stopping backend..."
                                );

                                scheduler.shutdown();

                                try {

                                    if (!scheduler.awaitTermination(
                                            5,
                                            TimeUnit.SECONDS
                                    )) {

                                        scheduler.shutdownNow();
                                    }

                                } catch (
                                        InterruptedException
                                                exception
                                ) {

                                    scheduler.shutdownNow();

                                    Thread.currentThread()
                                            .interrupt();
                                }

                                server.stop(0);

                                DBConnection
                                        .closeConnection();

                                System.out.println(
                                        "Backend stopped."
                                );
                            })
                    );

            server.start();

            System.out.println(
                    "MongoDB connected successfully."
            );

            System.out.println(
                    "Backend started on port "
                            + port
            );

            System.out.println(
                    "Email notification scheduler started."
            );

        } catch (Exception exception) {

            scheduler.shutdownNow();

            System.err.println(
                    "Server failed to start: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            DBConnection.closeConnection();

            System.exit(1);
        }
    }

    private static int getPort() {

        String portValue =
                System.getenv("PORT");

        if (portValue == null
                || portValue.isBlank()) {

            return 8080;
        }

        try {

            return Integer.parseInt(
                    portValue
            );

        } catch (NumberFormatException exception) {

            System.err.println(
                    "Invalid PORT value. "
                            + "Using port 8080."
            );

            return 8080;
        }
    }

    private static void handleHealthCheck(
            HttpExchange exchange
    ) throws IOException {

        if (CorsUtil.handleOptions(exchange)) {
            return;
        }

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

        ResponseUtil.sendMessage(
                exchange,
                200,
                "success",
                "Food Expiry API is running"
        );
    }
}