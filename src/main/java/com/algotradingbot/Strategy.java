package com.algotradingbot;

import java.util.ArrayList;

public class Strategy {

    private static final double SL_BUFFER = 20.0;
    private static final double ENTRY_BUFFER = 20.0;
    private static final double RISK_PER_TRADE_USD = 20.0;

    private final ArrayList<Candle> candles;
    private final ArrayList<Signal> signals;
    private final int SMA_DAYS = 20;

    public Strategy(ArrayList<Candle> candles) {
        this.candles = candles;
        signals = new ArrayList<>();
    }

    public void runBackTest() {

        for (int i = SMA_DAYS + 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);

            // כל עוד אף אחת מהתבניות לא מתקיימת – ממש הלאה
            if (!(isInsideBar(prev, curr) || isDoji(curr) || isHammer(curr))) {
                continue;
            }

            // מסנן נרות לא ירוקים
            if (curr.getClose() <= curr.getOpen()) {
                continue;
            }

            // יחס גוף לטווח
            double bodyToRange = (curr.getClose() - curr.getOpen()) / (curr.getHigh() - curr.getLow());
            if (bodyToRange < 0.5) {
                continue;
            }

            // ממוצע נע 50
            double sma50 = calculateSMA(i - 1, 50);
            if (curr.getClose() < sma50) {
                continue;
            }

            // התנאי הסופי לכניסה לפי SMA
            double sma = calculateSMA(i - 1, SMA_DAYS);
            if (prev.getClose() > sma) {
                signals.add(createBuySignal(i, curr));
                System.out.println("BUY on " + curr.getDate() + " at price: " + curr.getHigh());
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
