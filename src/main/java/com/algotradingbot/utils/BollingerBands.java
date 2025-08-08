package com.algotradingbot.utils;

public class BollingerBands {

    public enum BBPeriod {
        BB_20(20),
        BB_30(30),
        BB_50(50),
        BB_100(100);

        private final int period;

        BBPeriod(int period) {
            this.period = period;
        }

        public int getPeriod() {
            return period;
        }
    }

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
