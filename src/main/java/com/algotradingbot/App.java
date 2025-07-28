package com.algotradingbot;

import com.algotradingbot.engine.PeriodTester;

public class App {

    public static void main(String[] args) {
        PeriodTester.runTestsForMultiplePeriods("BTCUSDT", "15m");
    }

}
