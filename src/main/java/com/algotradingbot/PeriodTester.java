package com.algotradingbot;

import java.sql.Date;
import java.text.SimpleDateFormat;

public class PeriodTester {

    public static void runTestsForMultiplePeriods(String symbol, String interval) {
        long[][] crisisPeriods = {
            {1580515200000L, 1585699200000L}, // פברואר–מרץ 2020
            {1604188800000L, 1609459200000L}, // נובמבר–דצמבר 2020
            {1646092800000L, 1648771200000L}, // מרץ 2022
            {1672444800000L, 1675123200000L}, // ינואר 2023
        };

        long[][] bullMarketPeriods = {
            {1601510400000L, 1604188800000L}, // אוקטובר 2020
            {1612137600000L, 1614556800000L}, // פברואר 2021
        };

        long[][] bearMarketPeriods = {
            {1622505600000L, 1625097600000L}, // יוני 2021
            {1654041600000L, 1656633600000L}, // יוני 2022
        };

        long[][] normalPeriods = {
            {1680307200000L, 1682899200000L}, // אפריל–יוני 2023
            {1704067200000L, 1706745600000L}, // ינואר 2024
            {1711929600000L, 1714608000000L}, // אפריל 2024
        };

        runPeriodGroup("🦠 תקופות משבר", crisisPeriods, symbol, interval);
        runPeriodGroup("📈 תקופות עליות", bullMarketPeriods, symbol, interval);
        runPeriodGroup("📉 תקופות ירידות", bearMarketPeriods, symbol, interval);
        runPeriodGroup("📆 תקופות רגילות", normalPeriods, symbol, interval);
    }

    private static void runPeriodGroup(String title, long[][] periods, String symbol, String interval) {
        System.out.println("\n============ " + title + " ============\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (int i = 0; i < periods.length; i++) {
            long start = periods[i][0];
            long end = periods[i][1];

            System.out.printf("🔎 Test #%d | Period: %s ➝ %s\n", i + 1, sdf.format(new Date(start)), sdf.format(new Date(end)));

            try {
                String json = getDataFromBinance.fetchKlinesRange(symbol, interval, start, end);
                BacktestEngine bte = new BacktestEngine();
                bte.parseCandles(json);

                Strategy strategy = new Strategy(bte.getCandles());
                strategy.runBackTest();
                strategy.evaluteSignals();
                strategy.showResult();

                System.out.println("-------------------------------------------\n");

            } catch (Exception e) {
                System.err.println("❌ Error during test #" + (i + 1) + ": " + e.getMessage());
            }
        }
    }
}
