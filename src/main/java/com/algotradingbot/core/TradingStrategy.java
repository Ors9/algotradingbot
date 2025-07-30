package com.algotradingbot.core;

import java.util.ArrayList;

public abstract class TradingStrategy {

    protected ArrayList<Candle> candles;
    protected ArrayList<Signal> signals = new ArrayList<>();
    protected double riskPerTradeUSD;
    protected double riskReward;

    public TradingStrategy(ArrayList<Candle> candles) {
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

    public ArrayList<Candle> getCandles() {
        return candles;
    }

    public StrategyPerformance evaluatePerformance() {
        // Long performance variables
        int longWins = 0, longLosses = 0;
        double longProfit = 0, longBalance = 0, longPeak = 0, longMaxDD = 0;

        // Short performance variables
        int shortWins = 0, shortLosses = 0;
        double shortProfit = 0, shortBalance = 0, shortPeak = 0, shortMaxDD = 0;

        double commissionRate = 0.001; // 0.1%

        for (Signal signal : signals) {
            if (!signal.isEvaluated()) {
                continue;
            }

            double stopSize = Math.abs(signal.getEntryPrice() - signal.getStopPrice());
            if (stopSize == 0) {
                continue;
            }

            double positionSize = riskPerTradeUSD / stopSize;

            double entry = signal.getEntryPrice();
            double exit = signal.isWinSignal() ? signal.getTpPrice() : signal.getStopPrice();
            double profit;

            if (signal.isLong()) {
                profit = (exit - entry) * positionSize;
            } else {
                profit = (entry - exit) * positionSize;
            }

            // Commission
            double entryFee = entry * positionSize * commissionRate;
            double exitFee = exit * positionSize * commissionRate;
            profit -= (entryFee + exitFee);

            // Update per direction
            if (signal.isLong()) {
                longBalance += profit;
                longPeak = Math.max(longPeak, longBalance);
                longMaxDD = Math.max(longMaxDD, longPeak - longBalance);
                longProfit += profit;

                if (signal.isWinSignal()) {
                    longWins++; 
                }else {
                    longLosses++;
                }
            } else {
                shortBalance += profit;
                shortPeak = Math.max(shortPeak, shortBalance);
                shortMaxDD = Math.max(shortMaxDD, shortPeak - shortBalance);
                shortProfit += profit;

                if (signal.isWinSignal()) {
                    shortWins++; 
                }else {
                    shortLosses++;
                }
            }
        }

        Performance longPerf = new Performance(longWins, longLosses, longProfit, longMaxDD);
        Performance shortPerf = new Performance(shortWins, shortLosses, shortProfit, shortMaxDD);

        return new StrategyPerformance(longPerf, shortPerf);
    }

    public void printSignals() {
        for (Signal signal : signals) {
            System.out.println(signal);
        }
    }

}
