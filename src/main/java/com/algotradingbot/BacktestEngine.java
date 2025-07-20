package com.algotradingbot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class BacktestEngine {

    private final ArrayList<Candle> records;

    public BacktestEngine() {
        records = new ArrayList<>();
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

    public ArrayList<Candle> getCandles(){
        return records;
    }
}
