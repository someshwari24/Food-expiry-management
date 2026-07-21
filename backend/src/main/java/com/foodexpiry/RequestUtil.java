package com.foodexpiry;

import com.sun.net.httpserver.HttpExchange;
import org.bson.Document;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestUtil {

    private RequestUtil() {
    }

    public static String readRequestBody(
            HttpExchange exchange
    ) throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    public static Document readJson(
            HttpExchange exchange
    ) throws IOException {

        String body = readRequestBody(exchange);

        if (body == null || body.isBlank()) {
            return new Document();
        }

        return Document.parse(body);
    }

    public static Map<String, String> getQueryParameters(
            HttpExchange exchange
    ) {

        Map<String, String> parameters = new HashMap<>();

        String query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isBlank()) {
            return parameters;
        }

        String[] values = query.split("&");

        for (String value : values) {

            String[] pair = value.split("=", 2);

            String key = URLDecoder.decode(
                    pair[0],
                    StandardCharsets.UTF_8
            );

            String parameterValue = "";

            if (pair.length == 2) {
                parameterValue = URLDecoder.decode(
                        pair[1],
                        StandardCharsets.UTF_8
                );
            }

            parameters.put(key, parameterValue);
        }

        return parameters;
    }
}