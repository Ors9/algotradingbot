package com.algotradingbot;

/**
 * Fetches market data from Binance.
 * Includes functions for getting current prices and historical candlestick (kline) data.
 */
public class DataFetcher {
    BinanceApiClient apiClient = new BinanceApiClient();

    public String getCurrentBTCPrice() {
        String endpoint = "/api/v3/ticker/price?symbol=BTCUSDT";
        return apiClient.sendPublicGetRequest(endpoint);
    }
}
