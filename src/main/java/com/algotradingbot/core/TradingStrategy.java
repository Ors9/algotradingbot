package com.algotradingbot.core;

import java.util.ArrayList;


public abstract class TradingStrategy {

    protected ArrayList<Candle> candles;
    protected ArrayList<Signal> signals = new ArrayList<>();
    protected double riskPerTradeUSD;
    protected double riskReward;

    public TradingStrategy(ArrayList<Candle> candles ) {
        this.candles = candles;
        signals = new ArrayList<>();
    }

    /*Must implements */
    public abstract void runBackTest();

    public void evaluateSignals() {
        for (Signal signal : signals) {
            signal.evaluateSignal(signal, candles);
        }
    }

    public ArrayList<Signal> getSignals() {
        return signals;
    }

    public ArrayList<Candle> getCandles(){
        return candles;
    }



    public Performance evaluatePerformance() {
        int winCount = 0;
        int lossCount = 0;
        double totalProfit = 0;
        double runningBalance = 0;
        double peakBalance = 0;
        double maxDrawdown = 0;

        double commissionRate = 0.001; // 0.1% עמלה לכל צד

        for (Signal signal : signals) {
            if (signal.isEvaluated()) {
                double stopSize = Math.abs(signal.getEntryPrice() - signal.getStopPrice());
                if (stopSize == 0) {
                    continue;
                }

                double positionSize = riskPerTradeUSD / stopSize;

                double entryPrice = signal.getEntryPrice();
                double exitPrice = signal.isWinSignal()
                        ? signal.getTpPrice()
                        : signal.getStopPrice();

                double profitPerTrade = (exitPrice - entryPrice) * positionSize;

                // ✳️ חישוב עמלות
                double entryCommission = entryPrice * positionSize * commissionRate;
                double exitCommission = exitPrice * positionSize * commissionRate;
                double totalCommission = entryCommission + exitCommission;

                profitPerTrade -= totalCommission;

                // ✳️ עדכון ביצועים
                runningBalance += profitPerTrade;
                peakBalance = Math.max(peakBalance, runningBalance);
                maxDrawdown = Math.max(maxDrawdown, peakBalance - runningBalance);
                totalProfit += profitPerTrade;

                if (signal.isWinSignal()) {
                    winCount++;
                } else {
                    lossCount++;
                }
            }
        }

        return new Performance(winCount, lossCount, totalProfit, maxDrawdown);
    }

    public void printSignals() {
        for (Signal signal : signals) {
            System.out.println(signal);
        }
    }

}
