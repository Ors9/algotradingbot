package com.algotradingbot.engine;

import java.sql.Date;
import java.text.SimpleDateFormat;

import com.algotradingbot.chart.CandleChart;
import com.algotradingbot.core.StrategyPerformance;
import com.algotradingbot.strategies.OldInsideBarStrategy;
import com.algotradingbot.strategies.TrendRSIBBBand;

public class PeriodTester {

    public static void runSinglePeriodTest(String symbol, String interval, long start, long end) {
        System.out.println("\n============ SINGLE BIG PERIOD TEST ============\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        System.out.printf("Period: %s ➝ %s\n", sdf.format(new Date(start)), sdf.format(new Date(end)));

        try {
            String json = getDataFromBinance.fetchKlinesRange(symbol, interval, start, end);
            CandlesEngine bte = new CandlesEngine();
            bte.parseCandles(json);

            StrategyPerformance perf = testTrendRSIBBBand(bte);
            System.out.println("Results for Strategy:");
            perf.print();
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
        }
    }

    public static void runTestsForMultiplePeriods(String symbol, String interval) {
        long[][] crisisPeriods = {
            {1563235200000L, 1602979200000L}, // אוגוסט 2019 – אוקטובר 2020
            {1583020800000L, 1615766400000L}, // מרץ 2020 – מרץ 2021
            {1624406400000L, 1657152000000L}, // יולי 2021 – יולי 2022
            {1644854400000L, 1677600000000L}, // פברואר 2022 – פברואר 2023
        };

        long[][] dashMarketPeriods = {
            {1670198400000L, 1702944000000L} // דצמבר 2022 – דצמבר 2023
        };

        long[][] bullMarketPeriods = {
            {1596240000000L, 1628985600000L}, // אוגוסט 2020 – אוגוסט 2021
            {1609459200000L, 1642204800000L}, // ינואר 2021 – ינואר 2022
        };

        long[][] bearMarketPeriods = {
            {1617235200000L, 1649980800000L}, // אפריל 2021 – אפריל 2022
            {1636761600000L, 1669507200000L}, // נובמבר 2021 – נובמבר 2022
        };

        long[][] normalPeriods = {
            {1665014400000L, 1697760000000L}, // אוקטובר 2022 – אוקטובר 2023
            {1686787200000L, 1719532800000L}, // יוני 2023 – יוני 2024
            {1694649600000L, 1727395200000L}, // ספטמבר 2023 – ספטמבר 2024
        };

        StrategyPerformance all = null;

        StrategyPerformance group;

        group = runPeriodGroup(" Corona Time crisisPeriods", crisisPeriods, symbol, interval);
        all = all == null ? group : all.add(group);

        group = runPeriodGroup(" Bull period up market", bullMarketPeriods, symbol, interval);
        all = all.add(group);

        group = runPeriodGroup(" Bear market down market ", bearMarketPeriods, symbol, interval);
        all = all.add(group);

        group = runPeriodGroup(" Regular market ", normalPeriods, symbol, interval);
        all = all.add(group);

        group = runPeriodGroup(" Dash Market - Flat Range XRP", dashMarketPeriods, symbol, interval);
        all = all.add(group);

        System.out.println("\n ============OVERALL TOTAL PERFORMANCE ACROSS ALL PERIODS:=============");
        all.print();
    }

    private static StrategyPerformance runPeriodGroup(String title, long[][] periods, String symbol, String interval) {
        System.out.println("\n============ " + title + " ============\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        StrategyPerformance groupPerformance = null;

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
                StrategyPerformance perf2 = testTrendRSIBBBand(bte);
                System.out.println("Results for DashMarketStrategy:");
                perf2.print();
                if (groupPerformance == null) {
                    groupPerformance = perf2;
                } else {
                    groupPerformance = groupPerformance.add(perf2);
                }

                System.out.println("-------------------------------------------\n");

            } catch (Exception e) {
                System.err.println("Error during test #" + (i + 1) + ": " + e.getMessage());
            }
        }

        System.out.println(" TOTAL PERFORMANCE for " + title + ":");
        groupPerformance.print();
        return groupPerformance;
    }

    private static StrategyPerformance testOldInsideBarStrategy(CandlesEngine bte) {
        OldInsideBarStrategy strategy = new OldInsideBarStrategy(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        //strategy.printSignals();
        StrategyPerformance perf = strategy.evaluatePerformance();
        CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance());
        return strategy.evaluatePerformance();
    }

    private static StrategyPerformance testTrendRSIBBBand(CandlesEngine bte) {
        TrendRSIBBBand strategy = new TrendRSIBBBand(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        //strategy.printSignals();
        StrategyPerformance perf = strategy.evaluatePerformance();
        CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance());
        return strategy.evaluatePerformance();
    }
}
