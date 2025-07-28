package com.algotradingbot.utils;

public class BollingerBands {

    public double sma;
    public double upper;
    public double lower;

    public BollingerBands(double sma, double upper, double lower) {
        this.sma = sma;
        this.upper = upper;
        this.lower = lower;
    }
}
