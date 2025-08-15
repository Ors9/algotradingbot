package com.algotradingbot.core;

import java.util.ArrayList;

public class Signal {

    private final int indexInCandleList;               // מיקום האות ברשימת הנרות
    private final double entryPrice;
    private final double stopPrice;
    private final double tpPrice;
    private final double posSize20USD;
    private final boolean isLong;
    private boolean winSignal;      // true = הצלחה, false = כישלון
    private boolean evaluated;      // האם נותח כבר

    public Signal(int index, double entryPrice, double tpPrice, double stopPrice, double riskUsd, boolean isLong) {
        this.indexInCandleList = index;
        this.entryPrice = entryPrice;
        this.tpPrice = tpPrice;
        this.stopPrice = stopPrice;
        this.evaluated = false;
        this.isLong = isLong;

        double riskPerUnit = Math.abs(entryPrice - stopPrice);
        this.posSize20USD = riskPerUnit > 0 ? riskUsd / riskPerUnit : 0;
    }

    public double getPosSize20USD() {
        return posSize20USD;
    }

    public boolean isLong() {
        return isLong;
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
                + ", isLong= " + isLong
                + '}';
    }

    public void evaluateSignal(Signal signal, ArrayList<Candle> candles) {
        int startIndex = signal.getIndex() + 1;

        for (int i = startIndex; i < candles.size(); i++) {
            Candle c = candles.get(i);

            if (signal.isLong()) {
                // LONG: רוצים שהמחיר יעלה
                if (c.getLow() <= signal.getStopPrice()) {
                    signal.setWinSignal(false);
                    return;
                } else if (c.getHigh() >= signal.getTpPrice()) {
                    signal.setWinSignal(true);
                    return;
                }
            } else {
                // SHORT: רוצים שהמחיר ירד
                if (c.getHigh() >= signal.getStopPrice()) {
                    signal.setWinSignal(false);
                    return;
                } else if (c.getLow() <= signal.getTpPrice()) {
                    signal.setWinSignal(true);
                    return;
                }
            }
        }
    }

}
