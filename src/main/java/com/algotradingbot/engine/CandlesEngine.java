package com.algotradingbot.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;

import com.algotradingbot.core.Candle;

public class CandlesEngine {

    private final ArrayList<Candle> records;

    public CandlesEngine() {
        records = new ArrayList<>();

    }

    public void parseCandles(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            records.clear(); // ננקה את הרשימה לפני מילוי חדש

            for (int i = 0; i < arr.length(); i++) {
                JSONArray c = arr.getJSONArray(i);

                String date = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(new java.util.Date(c.getLong(0)));

                double open = Double.parseDouble(c.getString(1));
                double high = Double.parseDouble(c.getString(2));
                double low = Double.parseDouble(c.getString(3));
                double close = Double.parseDouble(c.getString(4));

                records.add(new Candle(date, open, high, low, close));
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Binance JSON: " + e.getMessage());
        }
    }

    public void loadCsv(String filePath) {
        BufferedReader br = null;
        boolean isFirstLine = true;
        try {
            br = new BufferedReader(new FileReader(filePath));
            String line;
            Candle candle;
            while ((line = br.readLine()) != null) {
                // Skip header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] val = line.split("[\\s]+");
                try {
                    candle = new Candle(val[0],
                            Double.parseDouble(val[1]),
                            Double.parseDouble(val[2]),
                            Double.parseDouble(val[3]),
                            Double.parseDouble(val[4]));
                    records.add(candle);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid numbers: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + filePath);
            }
        }
    }

    public void printRecords() {
        for (Candle record : records) {
            System.out.println(record);
        }
    }

    public ArrayList<Candle> getCandles() {
        return records;
    }
}
