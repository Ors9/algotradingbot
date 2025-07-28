package com.algotradingbot.engine;

import java.sql.Date;
import java.text.SimpleDateFormat;

import com.algotradingbot.core.Performance;
import com.algotradingbot.strategies.DashMarketStrategy;
import com.algotradingbot.strategies.OldInsideBarStrategy;

public class PeriodTester {

    public static void runTestsForMultiplePeriods(String symbol, String interval) {
        long[][] crisisPeriods = {
            {1580515200000L, 1585699200000L}, // פברואר–מרץ 2020
            {1604188800000L, 1609459200000L}, // נובמבר–דצמבר 2020
            {1646092800000L, 1648771200000L}, // מרץ 2022
            {1672444800000L, 1675123200000L}, // ינואר 2023
        };

        long[][] dashMarketPeriods = {
            {1685923200000L, 1687737600000L} // 5–26 ביוני 2023 (XRPUSDT היה בריינג' אופייני)
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

        Performance all = new Performance(0, 0, 0, 0);

        all = all.add(runPeriodGroup(" Corona Time crisisPeriods", crisisPeriods, symbol, interval));
        all = all.add(runPeriodGroup(" Bull period up market", bullMarketPeriods, symbol, interval));
        all = all.add(runPeriodGroup(" Bear market down market ", bearMarketPeriods, symbol, interval));
        all = all.add(runPeriodGroup(" Regular market ", normalPeriods, symbol, interval));
        all = all.add(runPeriodGroup(" Dash Market - Flat Range XRP", dashMarketPeriods, symbol, interval));


        System.out.println("\n ============OVERALL TOTAL PERFORMANCE ACROSS ALL PERIODS:=============");
        all.print();
    }

    private static Performance runPeriodGroup(String title, long[][] periods, String symbol, String interval) {
        System.out.println("\n============ " + title + " ============\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Performance groupPerformance = new Performance(0, 0, 0, 0);

        for (int i = 0; i < periods.length; i++) {
            long start = periods[i][0];
            long end = periods[i][1];

            System.out.printf(" Test #%d | Period: %s ➝ %s\n", i + 1, sdf.format(new Date(start)), sdf.format(new Date(end)));

            try {
                String json = getDataFromBinance.fetchKlinesRange(symbol, interval, start, end);
                CandlesEngine bte = new CandlesEngine();
                bte.parseCandles(json);

                /*Performance perf1 = testOldInsideBarStrategy(bte);
                System.out.println("Results for OldInsideBarStrategy:");
                perf1.print();
                groupPerformance = groupPerformance.add(perf1);*/
                Performance perf2 = testDashMarketStrategy(bte);
                System.out.println("Results for DashMarketStrategy:");
                perf2.print();
                groupPerformance = groupPerformance.add(perf2);

                System.out.println("-------------------------------------------\n");

            } catch (Exception e) {
                System.err.println("Error during test #" + (i + 1) + ": " + e.getMessage());
            }
        }

        System.out.println(" TOTAL PERFORMANCE for " + title + ":");
        groupPerformance.print();
        return groupPerformance;
    }

    private static Performance testOldInsideBarStrategy(CandlesEngine bte) {
        OldInsideBarStrategy strategy = new OldInsideBarStrategy(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        //strategy.printSignals();
        return strategy.evaluatePerformance();
    }

    private static Performance testDashMarketStrategy(CandlesEngine bte) {
        DashMarketStrategy strategy = new DashMarketStrategy(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        //strategy.printSignals();
        return strategy.evaluatePerformance();
    }
}
