package com.algotradingbot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App {

    public static void main(String[] args) {
        DataFetcher fetcher = new DataFetcher();
        String priceJson = fetcher.getCurrentBTCPrice();
        System.out.println("BTC Price Response: " + priceJson);
    }
}
