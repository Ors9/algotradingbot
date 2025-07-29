package com.algotradingbot;

import com.algotradingbot.engine.PeriodTester;

public class App {

    public static void main(String[] args) {
        //PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "15m");
        //PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "1h");
        long start = 1609459200000L; // 2021-01-01 00:00:00 UTC
        long end = 1735689600000L; // 2026-01-01 00:00:00 UTC
        PeriodTester.runSinglePeriodTest("BTCUSDT", "15m", start, end);
    }

}
