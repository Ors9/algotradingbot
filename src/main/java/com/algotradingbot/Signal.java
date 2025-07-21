package com.algotradingbot;

import java.util.ArrayList;

public class Signal {

    private final int indexInCandleList;               // מיקום האות ברשימת הנרות
    private final double entryPrice;
    private final double stopPrice;
    private final double tpPrice;
    private boolean winSignal;      // true = הצלחה, false = כישלון
    private boolean evaluated;      // האם נותח כבר

    public Signal(int index, double entryPrice, double tpPrice, double stopPrice) {
        this.indexInCandleList = index;
        this.entryPrice = entryPrice;
        this.tpPrice = tpPrice;       // ✔️ ערך מוחלט, לא מכפלה
        this.stopPrice = stopPrice;   // ✔️ גם ערך מוחלט
        this.evaluated = false;
    }

    // ===== Getters & Setters =====
    public int getIndex() {
        return indexInCandleList;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public double getTpPrice() {
        return tpPrice;
    }

    public boolean isWinSignal() {
        return winSignal;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setWinSignal(boolean winSignal) {
        this.winSignal = winSignal;
        this.evaluated = true;
    }

    @Override
    public String toString() {
        return "Signal{"
                + "index=" + indexInCandleList
                + ", entryPrice=" + entryPrice
                + ", tpPrice=" + tpPrice
                + ", stopPrice=" + stopPrice
                + ", winSignal=" + winSignal
                + ", evaluated=" + evaluated
                + '}';
    }

    public void evaluteSignal(Signal signal, ArrayList<Candle> candles) {
        int startIndex = signal.getIndex();
        for (int i = startIndex; i < candles.size(); i++) {
            Candle c = candles.get(i);

            if (c.getHigh() >= signal.getTpPrice()) {
                signal.setWinSignal(true);
                return;
            } else if (c.getLow() <= signal.getStopPrice()) {
                signal.setWinSignal(false);
                return;
            }
        }
    }

}
