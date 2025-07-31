package com.algotradingbot;

import com.algotradingbot.engine.PeriodTester;

public class App {

    public static void main(String[] args) {
        //PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "15m");
        //PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "1h");
        long start = 1483228800000L; // 2017-01-01 00:00:00 UTC
        long end = System.currentTimeMillis(); // עכשיו
        PeriodTester.runSinglePeriodTest("BTCUSDT", "1h", start, end); //almost 9 years
        //BTCUSDT  ETHUSDT  BNBUSDT   XRPUSDT SOLUSDT
    }

}
