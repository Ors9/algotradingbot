package com.algotradingbot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Main entry point for the Algo Trading Bot.
 * Initializes components and starts the bot scheduler.
 */
public class App {

    public static void main(String[] args) {
        String url = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
