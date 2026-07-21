package com.foodexpiry;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class CorsUtil {

    private CorsUtil() {
    }

    public static void addCorsHeaders(HttpExchange exchange) {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin",
                "*"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization"
        );

        exchange.getResponseHeaders().set(
                "Access-Control-Max-Age",
                "86400"
        );
    }

    public static boolean handleOptions(
            HttpExchange exchange
    ) throws IOException {

        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }

        return false;
    }
}