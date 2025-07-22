package com.algotradingbot;

import java.util.ArrayList;

public class Strategy {

    private static final double RISK_PER_TRADE_USD = 20.0;

    private final ArrayList<Candle> candles;
    private final ArrayList<Signal> signals;
    private final int SMA_DAYS_20 = 20;
    private final int SMA_DAYS_50 = 50;

    public Strategy(ArrayList<Candle> candles) {
        this.candles = candles;
        signals = new ArrayList<>();
    }

    public void runBackTest() {
        // We need at least 51 candles because strategy uses SMA(50)
        for (int i = SMA_DAYS_50 + 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);

            if (strategyInsideBar(prev, curr, i)) {
                signals.add(createBuySignal(i, curr));
            }
        }
    }

    private Signal createBuySignal(int index, Candle curr) {
        double entry = curr.getHigh() + 20;         // כניסה 20 דולר מעל high
        double sl = curr.getLow() - 20;             // SL 20 דולר מתחת ל־low
        double range = entry - sl;                  // מרחק בין כניסה ל־SL
        double tp = entry + 2 * range;                 // TP של 1:1
        return new Signal(index, entry, tp, sl);
    }

    private double calculateSMA(int index, int period) throws Exception {
        if (index - period + 1 < 0) {
            throw new IllegalArgumentException("Not enough candles to calculate SMA at index " + index);
        }
        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += candles.get(i).getClose();
        }
        return sum / period;
    }

    public static boolean isGreenCandle(Candle candle) {
        return candle.getClose() > candle.getOpen();
    }

    public boolean hasStrongBody(Candle curr) {
        double range = curr.getHigh() - curr.getLow();
        if (range == 0) {
            return false; // כדי למנוע חילוק באפס

        }
        double body = Math.abs(curr.getClose() - curr.getOpen());
        double bodyToRange = body / range;
        return bodyToRange >= 0.5;
    }

    public boolean strategyInsideBar(Candle prev, Candle curr, int index) {
        if (!isInsideBar(prev, curr)) {
            return false;
        }
        if (!isGreenCandle(curr)) {
            return false;
        }
        if (!hasStrongBody(curr)) {
            return false;
        }

        try {
            double sma50 = calculateSMA(index - 1, SMA_DAYS_50);
            if (sma50 == -1) {
                return false;
            }
            if (curr.getClose() < sma50) {
                return false;
            }

            double sma20 = calculateSMA(index - 1, SMA_DAYS_20);
            if (sma20 == -1) {
                return false;
            }
            if (curr.getClose() < sma20) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean isInsideBar(Candle prev, Candle curr) {
        double rangeMargin = 0.002 * prev.getHigh(); // מרווח של 0.2%
        return curr.getHigh() <= prev.getHigh() + rangeMargin && curr.getLow() >= prev.getLow() - rangeMargin;

    }

    public boolean isDoji(Candle candle) {
        double body = Math.abs(candle.getClose() - candle.getOpen());
        double range = candle.getHigh() - candle.getLow();
        return body <= range * 0.1; // גוף קטן מ־10% מהטווח
    }

    public boolean isHammer(Candle candle) {
        double body = Math.abs(candle.getClose() - candle.getOpen());
        double lowerWick = candle.getOpen() < candle.getClose()
                ? candle.getOpen() - candle.getLow()
                : candle.getClose() - candle.getLow();
        double upperWick = candle.getHigh() - Math.max(candle.getClose(), candle.getOpen());

        return lowerWick > 2 * body && upperWick < body;
    }

    public void evaluteSignals() {
        for (int i = 0; i < signals.size(); i++) {
            signals.get(i).evaluteSignal(signals.get(i), candles);
        }
    }

    public void showResult() {
        int winCount = 0;
        int lossCount = 0;
        double totalProfit = 0;

        System.out.println("========= Signal Results =========");

        for (Signal signal : signals) {
            System.out.println(signal);

            if (signal.isEvaluated()) {
                double stopSize = Math.abs(signal.getEntryPrice() - signal.getStopPrice());
                if (stopSize == 0) {
                    continue;
                }
                double positionSize = RISK_PER_TRADE_USD / stopSize;

                double profitPerTrade = signal.isWinSignal()
                        ? (signal.getTpPrice() - signal.getEntryPrice()) * positionSize
                        : (signal.getStopPrice() - signal.getEntryPrice()) * positionSize; // שלילי

                totalProfit += profitPerTrade;

                if (signal.isWinSignal()) {
                    winCount++;
                } else {
                    lossCount++;
                }
            }
        }

        int total = winCount + lossCount;
        double winRate = total > 0 ? 100.0 * winCount / total : 0;

        System.out.println("==================================");
        System.out.println("Total Signals Evaluated: " + total);
        System.out.println("Winners: " + winCount);
        System.out.println("Losers : " + lossCount);
        System.out.printf("Win Rate: %.2f%%\n", winRate);
        System.out.printf("Total Net Profit: $%.2f\n", totalProfit);
    }

}
