package com.foodexpiry;

import com.sun.net.httpserver.HttpExchange;
import org.bson.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {

    private ResponseUtil() {
    }

    public static void sendJson(
            HttpExchange exchange,
            int statusCode,
            String json
    ) throws IOException {

        CorsUtil.addCorsHeaders(exchange);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        byte[] responseBytes =
                json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length
        );

        try (OutputStream outputStream =
                     exchange.getResponseBody()) {

            outputStream.write(responseBytes);
        }
    }

    public static void sendMessage(
            HttpExchange exchange,
            int statusCode,
            String status,
            String message
    ) throws IOException {

        Document response = new Document()
                .append("status", status)
                .append("message", message);

        sendJson(
                exchange,
                statusCode,
                response.toJson()
        );
    }
}