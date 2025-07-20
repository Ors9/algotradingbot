package com.algotradingbot;

import java.util.ArrayList;

public class Strategy {

    private final ArrayList<Candle> candles;
    private final int SMA_DAYS = 20;

    public Strategy(ArrayList<Candle> candles){
        this.candles = candles;
    }

    public double runBackTest() {
        
        int signals = 0;

        for (int i = SMA_DAYS + 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);
            if(!isInsideBar(prev, curr)){
                continue;
            }
            double sma = calculateSMA(i - 1, SMA_DAYS);
            if (prev.getClose() > sma) {
                System.out.println("BUY on " + curr.getDate() + " at price: " + curr.getClose());
                signals++;
            }
        }
        System.out.println("Total buy signals: " + signals);
        return signals;
    }

    private double calculateSMA(int index, int period) {
        if (index < period) {
            return -1;
        }
        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += candles.get(i).getClose();
        }
        return sum / period;
    }

    public boolean isInsideBar(Candle prev, Candle curr) {
        return curr.getHigh() <= prev.getHigh() && curr.getLow() >= prev.getLow();
    }

}
