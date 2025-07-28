package com.algotradingbot.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONTokener;

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

    public static String fetchKlinesRange(String symbol, String interval, long startTime, long endTime) throws Exception {
        JSONArray allCandles = new JSONArray();
        long currentStart = startTime;

        while (currentStart < endTime) {
            String urlString = String.format(
                    "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=1000&startTime=%d",
                    symbol, interval, currentStart);

            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");

            int status = con.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("HTTP error: " + status);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();

            JSONArray batch = new JSONArray(new JSONTokener(response.toString()));
            if (batch.length() == 0) {
                break;
            }

            for (int i = 0; i < batch.length(); i++) {
                allCandles.put(batch.getJSONArray(i));
            }

            // Advance currentStart to the close time of the last candle
            long lastCloseTime = batch.getJSONArray(batch.length() - 1).getLong(6);
            currentStart = lastCloseTime + 1;
        }

        return allCandles.toString();
    }

}
