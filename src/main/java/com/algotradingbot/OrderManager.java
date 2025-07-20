package com.algotradingbot;

import java.util.Map;

/**
 * Responsible for placing, modifying, and canceling orders via Binance API.
 * Uses BinanceApiClient for sending trade orders.
 */
public class OrderManager {

    public String sendSignedPostRequest(String endpoint, Map<String, String> params) {
        // TODO: Build query string, add timestamp, sign with HMAC SHA256, send POST
        return null;
    }

    public void placeBuyOrder(String symbol, double quantity) {
        String endpoint = "/api/v3/order";
        // TODO: לבנות פרמטרים ל-Buy order (quantity, symbol, side=BUY, type=MARKET)
        System.out.println("Placing BUY order for " + quantity + " " + symbol);
        // apiClient.sendSignedPostRequest(endpoint, params);
    }

    public void placeSellOrder(String symbol, double quantity) {
        String endpoint = "/api/v3/order";
        // TODO: לבנות פרמטרים ל-Sell order (quantity, symbol, side=SELL, type=MARKET)
        System.out.println("Placing SELL order for " + quantity + " " + symbol);
        // apiClient.sendSignedPostRequest(endpoint, params);
    }

    public void placeStopLoss(String symbol, String quantity, String stopPrice) {
        String endpoint = "/api/v3/order";
        // TODO: לבנות פרמטרים ל-Stop Loss (side=SELL, type=STOP_LOSS_LIMIT)
        System.out.println("Placing STOP LOSS at " + stopPrice + " for " + symbol);
        // apiClient.sendSignedPostRequest(endpoint, params);
    }
}
