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

    public StrategyPerformance evaluatePerformanceEURUSD() {
        // Long performance variables
        int longWins = 0, longLosses = 0;
        double longProfit = 0, longBalance = 0, longPeak = 0, longMaxDD = 0;

        final double FIXED_FEE_PER_TRADE = 1.0;

        // Short performance variables
        int shortWins = 0, shortLosses = 0;
        double shortProfit = 0, shortBalance = 0, shortPeak = 0, shortMaxDD = 0;

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

            profit -= FIXED_FEE_PER_TRADE;

            // Update per direction
            if (signal.isLong()) {
                longBalance += profit;
                longPeak = Math.max(longPeak, longBalance);
                longMaxDD = Math.max(longMaxDD, longPeak - longBalance);
                longProfit += profit;

                if (signal.isWinSignal()) {
                    longWins++;
                } else {
                    longLosses++;
                }
            } else {
                shortBalance += profit;
                shortPeak = Math.max(shortPeak, shortBalance);
                shortMaxDD = Math.max(shortMaxDD, shortPeak - shortBalance);
                shortProfit += profit;

                if (signal.isWinSignal()) {
                    shortWins++;
                } else {
                    shortLosses++;
                }
            }
        }

        Performance longPerf = new Performance(longWins, longLosses, longProfit, longMaxDD);
        Performance shortPerf = new Performance(shortWins, shortLosses, shortProfit, shortMaxDD);

        return new StrategyPerformance(longPerf, shortPerf);
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
                } else {
                    longLosses++;
                }
            } else {
                shortBalance += profit;
                shortPeak = Math.max(shortPeak, shortBalance);
                shortMaxDD = Math.max(shortMaxDD, shortPeak - shortBalance);
                shortProfit += profit;

                if (signal.isWinSignal()) {
                    shortWins++;
                } else {
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

    public Signal createBuySignal(int index, Candle curr) {
        double entryBufferPct = 0.002; // 0.2% מעל השיא
        double stopLossPct = 0.002;     // 1% מתחת לשפל

        double highPrice = curr.getHigh();
        double lowPrice = curr.getLow();

        double entryPrice = highPrice * (1 + entryBufferPct);
        double stopLossPrice = lowPrice * (1 - stopLossPct);

        double riskPerUnit = entryPrice - stopLossPrice;

        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double takeProfitPrice = entryPrice + (riskReward * riskPerUnit);

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, true);
    }

    public Signal createBuySignalFromClose(int index, Candle curr) {
        double stopLossPct = 0.002;     // 1% מתחת לשפל

        double closePrice = curr.getClose();
        double lowPrice = curr.getLow();

        double entryPrice = closePrice;
        double stopLossPrice = lowPrice * (1 - stopLossPct);

        double riskPerUnit = entryPrice - stopLossPrice;

        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double takeProfitPrice = entryPrice + (riskReward * riskPerUnit);

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, true);
    }

    // SHORT: SL above entry by k*ATR; TP below entry by RR*k*ATR
    public Signal createSellSignalATR(int index, Candle curr, double atr, double atrMult, double rr) {
        if (Double.isNaN(atr) || atr <= 0 || atrMult <= 0 || rr <= 0) {
            return null;
        }

        double entryPrice = curr.getClose();
        double riskPerUnit = atrMult * atr;          // distance to SL
        double stopLossPrice = entryPrice + riskPerUnit;
        double takeProfitPrice = entryPrice - rr * riskPerUnit;

        if (riskPerUnit <= 0) {
            return null;
        }
        double positionSize = riskPerTradeUSD / riskPerUnit;

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, /*isLong=*/ false);
    }

// LONG: SL below entry by k*ATR; TP above entry by RR*k*ATR
    public Signal createBuySignalATR(int index, Candle curr, double atr, double atrMult, double rr) {
        if (Double.isNaN(atr) || atr <= 0 || atrMult <= 0 || rr <= 0) {
            return null;
        }

        double entryPrice = curr.getClose();
        double riskPerUnit = atrMult * atr;          // distance to SL
        double stopLossPrice = entryPrice - riskPerUnit;
        double takeProfitPrice = entryPrice + rr * riskPerUnit;

        if (riskPerUnit <= 0) {
            return null;
        }
        double positionSize = riskPerTradeUSD / riskPerUnit;

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, /*isLong=*/ true);
    }

    public Signal createSellSignalFromClose(int index, Candle curr) {
        double stopLossPct = 0.002; // 0.2% מעל השיא

        double closePrice = curr.getClose();
        double highPrice = curr.getHigh();

        double entryPrice = closePrice;
        double stopLossPrice = highPrice * (1 + stopLossPct);

        double riskPerUnit = stopLossPrice - entryPrice;

        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double takeProfitPrice = entryPrice - (riskReward * riskPerUnit);

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, false);
    }

    /*
    good for majors pip 0.0001 
    EURUSD,GBPUSD,AUDUSD,NZDUSD,USDCAD,USDCHF
     */
    public Signal createSellSignalATR_MajorForex(
            int index, Candle curr,
            double atrPrice, // ATR ביחידות מחיר (e.g., 0.0006 ≈ 6 pips)
            double atrMult,
            double riskReward) {

        if (curr == null || atrPrice <= 0 || Double.isNaN(atrPrice) || atrMult <= 0) {
            return null;
        }

        final double PIP = 0.0001;         // EURUSD pip size
        final double MIN_STOP_PIPS = 15;   // רצפת סטופ סבירה ל־1H/4H; שנה לפי טיימפריים
        // final double ENTRY_BUFFER_PIPS = 0; // אם תרצה בופר לכניסה

        double entry = curr.getClose(); // כניסה ב־close ל־SHORT; אפשר להפחית ENTRY_BUFFER_PIPS*PIP אם רוצים

        // המרה ממחיר→pips (אם ה-ATR אצלך כבר ב-pips, החלף ל: double atrPips = atrPrice;)
        double atrPips = atrPrice / PIP;

        double stopDistancePips = Math.max(atrPips * atrMult, MIN_STOP_PIPS);
        double stop = entry + stopDistancePips * PIP;  // סטופ מעל הכניסה (SHORT)
        double riskPerUnit = stop - entry;
        if (riskPerUnit <= 0) {
            return null;
        }

        double tp = entry - (riskReward * riskPerUnit); // RR (למשל 1.0 עבור 1:1)

        return new Signal(index, entry, tp, stop, riskPerTradeUSD, false);
    }

    /*
    good for majors pip 0.0001 
    EURUSD,GBPUSD,AUDUSD,NZDUSD,USDCAD,USDCHF
     */
    public Signal createBuySignalATR_MajorForex(
            int index, Candle curr,
            double atrPrice, // ATR in price units (e.g., 0.0006 ≈ 6 pips)
            double atrMult,
            double riskReward) {

        if (curr == null || atrPrice <= 0 || Double.isNaN(atrPrice) || atrMult <= 0) {
            return null;
        }

        final double PIP = 0.0001;         // EURUSD pip size
        final double MIN_STOP_PIPS = 15;   // floor for 1H/4H; tune per timeframe
        // final double ENTRY_BUFFER_PIPS = 0; // if you want a buffer on entry

        double entry = curr.getClose(); // enter at close for LONG

        // Convert ATR price → pips (if your ATR is already pips, use it directly)
        double atrPips = atrPrice / PIP;

        double stopDistancePips = Math.max(atrPips * atrMult, MIN_STOP_PIPS);
        double stop = entry - stopDistancePips * PIP;  // stop below entry (LONG)
        double riskPerUnit = entry - stop;
        if (riskPerUnit <= 0) {
            return null;
        }

        double tp = entry + (riskReward * riskPerUnit); // RR (e.g., 1.0 for 1:1)

        return new Signal(index, entry, tp, stop, riskPerTradeUSD, true);
    }

}
