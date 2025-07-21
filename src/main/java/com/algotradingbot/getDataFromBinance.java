package com.algotradingbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class getDataFromBinance {

    public static String fetchKlines(String symbol, String interval, int limit) throws Exception {
        String urlString = String.format(
                "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                symbol, interval, limit);

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");

        int status = con.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("HTTP error: " + status);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();
        con.disconnect();
        return response.toString();
    }
}
