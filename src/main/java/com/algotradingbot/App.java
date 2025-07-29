package com.algotradingbot;

import com.algotradingbot.engine.PeriodTester;

public class App {

    public static void main(String[] args) {
        //PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "15m");
        //PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "1h");
        long start = 1609459200000L; // 2021-01-01 00:00:00 UTC
        long end = 1640908800000L; // 2021-12-31 23:59:59 UTC (roughly)
        PeriodTester.runSinglePeriodTest("BTCUSDT", "1h", start, end);
    }

}
