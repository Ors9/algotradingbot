package com.algotradingbot.utils;


public class FilterRejectionTracker {

    private int totalEvaluatedLong = 0;
    private int totalEvaluatedShort = 0;

    private int rejectedRSILong = 0;
    private int rejectedRSIShort = 0;

    private int rejectedBBLong = 0;
    private int rejectedBBShort = 0;

    private int rejectedTrendLong = 0;
    private int rejectedTrendShort = 0;

    private int rejectedPatternLong = 0;
    private int rejectedPatternShort = 0;

    private int rejectedTimeLong = 0;
    private int rejectedTimeShort = 0;

    private int rejectedCandleLong = 0;
    private int rejectedCandleShort = 0;

    // 🔁 Total evaluations
    public void incrementTotal(boolean isLong) {
        if (isLong) {
            totalEvaluatedLong++;
        } else {
            totalEvaluatedShort++;
        }
    }

    // 🔁 RSI
    public void incrementRSI(boolean isLong) {
        if (isLong) {
            rejectedRSILong++;
        } else {
            rejectedRSIShort++;
        }
    }

    // 🔁 Bollinger
    public void incrementBB(boolean isLong) {
        if (isLong) {
            rejectedBBLong++;
        } else {
            rejectedBBShort++;
        }
    }

    // 🔁 Trend
    public void incrementTrend(boolean isLong) {
        if (isLong) {
            rejectedTrendLong++;
        } else {
            rejectedTrendShort++;
        }
    }

    // 🔁 Pattern
    public void incrementPattern(boolean isLong) {
        if (isLong) {
            rejectedPatternLong++;
        } else {
            rejectedPatternShort++;
        }
    }

    // 🔁 Candle body/color
    public void incrementCandle(boolean isLong) {
        if (isLong) {
            rejectedCandleLong++;
        } else {
            rejectedCandleShort++;
        }
    }

    // 🔁 Time filter
    public void incrementTime(boolean isLong) {
        if (isLong) {
            rejectedTimeLong++;
        } else {
            rejectedTimeShort++;
        }
    }

    public void print() {
        System.out.println("=== Filter Rejection Summary ===");

        System.out.println("\n==== LONG trades:=====");
        System.out.printf("Total signals evaluated:   %d\n", totalEvaluatedLong);
        System.out.printf(" Rejected by RSI:         %d\n", rejectedRSILong);
        System.out.printf(" Rejected by Bollinger:   %d\n", rejectedBBLong);
        System.out.printf(" Rejected by Trend:       %d\n", rejectedTrendLong);
        System.out.printf(" Rejected by Pattern:     %d\n", rejectedPatternLong);
        System.out.printf(" Rejected by Candle Body: %d\n", rejectedCandleLong);
        System.out.printf(" Rejected by Time Filter: %d\n", rejectedTimeLong);

        System.out.println("\n==== SHORT trades:====");
        System.out.printf("Total signals evaluated:   %d\n", totalEvaluatedShort);
        System.out.printf(" Rejected by RSI:         %d\n", rejectedRSIShort);
        System.out.printf(" Rejected by Bollinger:   %d\n", rejectedBBShort);
        System.out.printf(" Rejected by Trend:       %d\n", rejectedTrendShort);
        System.out.printf(" Rejected by Pattern:     %d\n", rejectedPatternShort);
        System.out.printf(" Rejected by Candle Body: %d\n", rejectedCandleShort);
        System.out.printf(" Rejected by Time Filter: %d\n", rejectedTimeShort);
    }
}
