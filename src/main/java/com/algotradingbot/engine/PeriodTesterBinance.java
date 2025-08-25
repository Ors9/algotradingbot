package com.algotradingbot.engine;

import java.sql.Date;
import java.text.SimpleDateFormat;

import com.algotradingbot.chart.CandleChart;
import com.algotradingbot.core.StrategyPerformance;
import com.algotradingbot.strategies.BBbandWithComma4HBTCUSDT;
import com.algotradingbot.strategies.MWPatternBTCUSDT1H;
import com.algotradingbot.strategies.OldInsideBarStrategy;
import com.algotradingbot.strategies.TrendRSIBBBand;

public class PeriodTesterBinance {

    public static void runSinglePeriodTestBinance(String symbol, String interval, long start, long end) {
        System.out.println("\n============ SINGLE BIG PERIOD TEST ============\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        System.out.printf("Period: %s ➝ %s\n", sdf.format(new Date(start)), sdf.format(new Date(end)));

        try {
            String json = GetDataFromBinance.fetchKlinesRange(symbol, interval, start, end);
            CandlesEngine bte = new CandlesEngine();
            bte.parseCandles(json);

            /*StrategyPerformance perf = testTrendRSIBBBand(bte);
            System.out.println("Results for Strategy:");
            perf.print();*/
 /*StrategyPerformance oldStrategy = testOldInsideBarStrategy(bte);
            System.out.println("Results for Strategy:");
            oldStrategy.print();*/
            //StrategyPerformance bbBandWithComma = testBBbandWithCommaStrategy(bte);
            //bbBandWithComma.print();
            
            StrategyPerformance mwPtrn = testMWPattern(bte);
            mwPtrn.print();

        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
        }
    }

    public static void runTestsForMultiplePeriods(String symbol, String interval) {
        // ⚡ מהלכים פרבוליים
        long[][] parabolicBullRuns = {
            {1512086400000L, 1514678400000L}, // דצמבר 2017 – שיא הביטקוין
            {1604188800000L, 1609459199000L}, // נובמבר 2020 – סוף דצמבר 2020 (לפני bullMarketPeriods)
        };

        // ⚠️ קריסות מהירות (שאינן חופפות)
        long[][] flashCrashes = {
            {1583971200000L, 1584230400000L}, // מרץ 2020
            {1622505600000L, 1622764800000L}, // יוני 2021
        };

        // ⏸️ שוק מדשדש
        long[][] longSidewaysPeriods = {
            {1654041600000L, 1661990400000L}, // יוני – ספטמבר 2022
        };

        // 📉 שוק תנודתי ומטלטל (לא כולל את ינואר 2022 שחופף ל־bear)
        long[][] volatileShakeouts = {
            {1640995200000L, 1642204799000L}, // ינואר 2022 עד לפני bull
        };

        // 💥 משברי שוק (שלא חופפים לפרבולי / בול)
        long[][] crisisPeriods = {
            {1563235200000L, 1583020799000L}, // אוגוסט 2019 – פברואר 2020
            {1584230401000L, 1596240000000L}, // אחרי crash עד תחילת bull (מרץ – אוגוסט 2020)
            {1628985600000L, 1640995199000L}, // סוף bull – דצמבר 2021
        };

        // 🟢 שוק שוורי
        long[][] bullMarketPeriods = {
            {1596240000000L, 1604188799000L}, // אוגוסט 2020 – אוקטובר 2020
            {1609459200000L, 1622505599000L}, // ינואר 2021 – סוף מאי 2021
        };

        // 🔴 שוק דובי (ללא חפיפה עם volatile או bull)
        long[][] bearMarketPeriods = {
            {1622764801000L, 1636761599000L}, // יוני 2021 – אוקטובר 2021
            {1661990401000L, 1669507199000L}, // אחרי sideways – נובמבר 2022
        };

        // ⚪ שוק נורמלי (לא שייך לשום קטגוריה אחרת)
        long[][] normalPeriods = {
            {1669507200000L, 1686787199000L}, // נובמבר 2022 – יוני 2023
            {1686787200000L, 1694649599000L}, // יוני – ספטמבר 2023
            {1694649600000L, 1727395200000L}, // ספטמבר 2023 – ספטמבר 2024
        };

        // 🚀 שוק DASH – לאחר כל האחרים
        long[][] dashMarketPeriods = {
            {1670198400000L, 1702944000000L} // אם תרצה להשאיר – רק להבטיח שלא חופף ל־normal
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

        group = runPeriodGroup("Parabolic Bull Runs", parabolicBullRuns, symbol, interval);

        all = all.add(group);

        group = runPeriodGroup("Flash Crashes", flashCrashes, symbol, interval);
        all = all.add(group);

        group = runPeriodGroup("Sideways Low Volatility", longSidewaysPeriods, symbol, interval);
        all = all.add(group);

        group = runPeriodGroup("Volatile Shakeouts", volatileShakeouts, symbol, interval);
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
                String json = GetDataFromBinance.fetchKlinesRange(symbol, interval, start, end);
                CandlesEngine bte = new CandlesEngine();
                bte.parseCandles(json);

                /*   StrategyPerformance perf1 = testOldInsideBarStrategy(bte);
                System.out.println("Results for OldInsideBarStrategy:");
                perf1.print();
                if (groupPerformance == null) {
                    groupPerformance = perf1;
                } else {
                    groupPerformance = groupPerformance.add(perf1);
                }*/
                /* 
                StrategyPerformance bbBandWithComma = testBBbandWithCommaStrategy(bte);
                bbBandWithComma.print();
                if (groupPerformance == null) {
                    groupPerformance = bbBandWithComma;
                } else {
                    groupPerformance = groupPerformance.add(bbBandWithComma);
                }*/
                
                StrategyPerformance mwPtrn = testMWPattern(bte);
                mwPtrn.print();
                if (groupPerformance == null) {
                    groupPerformance = mwPtrn;
                } else {
                    groupPerformance = groupPerformance.add(mwPtrn);
                }

                /*StrategyPerformance perf2 = testTrendRSIBBBand(bte);
                System.out.println("Results for DashMarketStrategy:");
                perf2.print();*/
 /* 
                if (groupPerformance == null) {
                    groupPerformance = perf2;
                } else {
                    groupPerformance = groupPerformance.add(perf2);
                }*/
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
        CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance(), CandleChart.ChartOverlayMode.TREND_RSI_BB);
        return strategy.evaluatePerformance();
    }

    private static StrategyPerformance testBBbandWithCommaStrategy(CandlesEngine bte) {
        BBbandWithComma4HBTCUSDT strategy = new BBbandWithComma4HBTCUSDT(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        //strategy.printSignals();
        StrategyPerformance perf = strategy.evaluatePerformance();
        CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance(), CandleChart.ChartOverlayMode.BB_COMMA_ONLY);
        return strategy.evaluatePerformance();
    }

    private static StrategyPerformance testTrendRSIBBBand(CandlesEngine bte) {
        TrendRSIBBBand strategy = new TrendRSIBBBand(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        strategy.printSignals();
        StrategyPerformance perf = strategy.evaluatePerformance();
        CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance(), CandleChart.ChartOverlayMode.TREND_RSI_BB);
        return strategy.evaluatePerformance();
    }

    private static StrategyPerformance testMWPattern(CandlesEngine bte) {
        MWPatternBTCUSDT1H strategy = new MWPatternBTCUSDT1H(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluateSignals();
        //strategy.printSignals();
        StrategyPerformance perf = strategy.evaluatePerformance();
        CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance(), CandleChart.ChartOverlayMode.DIVERGENCE);
        return strategy.evaluatePerformance();
    }

}
