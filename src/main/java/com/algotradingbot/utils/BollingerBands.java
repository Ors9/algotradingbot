package com.algotradingbot.utils;

public class BollingerBands {


    private final double sma;
    private final double upper;
    private final double lower;

    public BollingerBands(double sma, double upper, double lower) {
        this.sma = sma;
        this.upper = upper;
        this.lower = lower;
    }

    public double getSma() {
        return sma;
    }

    public double getUpper() {
        return upper;
    }

    public double getLower() {
        return lower;
    }
}
