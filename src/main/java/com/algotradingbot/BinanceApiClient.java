package com.algotradingbot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Handles low-level communication with Binance API.
 * Responsible for sending HTTP GET/POST requests and signing parameters when needed.
 */
public class BinanceApiClient {
    public String sendPublicGetRequest(String endpoint) {
        String url = Config.BASE_URL + endpoint;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
