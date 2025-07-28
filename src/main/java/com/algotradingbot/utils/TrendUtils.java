package com.algotradingbot.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.algotradingbot.core.Candle;


public class TrendUtils {

    public static boolean isStrongBullMarket(ArrayList<Candle> candles, int index, int sma20, int sma50, int sma200) {
        try {
            double sma200Val = calculateSMA(candles, index, sma200);
            double sma50Val = calculateSMA(candles, index, sma50);
            double sma20Val = calculateSMA(candles, index, sma20);
            double price = candles.get(index).getClose();

            return sma20Val > sma50Val && sma50Val > sma200Val && price > sma20Val;
        } catch (Exception e) {
            return false;
        }
    }

    public static double calculateSMA(ArrayList<Candle> candles, int index, int period) throws Exception {
        if (index - period + 1 < 0) {
            throw new IllegalArgumentException("Not enough candles");
        }

        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += candles.get(i).getClose();
        }
        return sum / period;
    }

    public static boolean isHighTFBullTrend(Candle candle, String symbol, int sma20Period, int sma50Period, int sma200Period) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime candleTime = LocalDateTime.parse(candle.getDate(), formatter);
            TFtrendAnalyzer tfa = new TFtrendAnalyzer(candleTime, symbol, "1d");

            double sma200 = tfa.smaCalc(sma200Period);
            double sma50 = tfa.smaCalc(sma50Period);
            double sma20 = tfa.smaCalc(sma20Period);
            double price = candle.getClose();

            return sma20 > sma50 && sma50 > sma200 && price > sma20;

        } catch (Exception e) {
            System.err.println("High TF trend check failed: " + e.getMessage());
            return false;
        }
    }
}
