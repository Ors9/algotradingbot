package com.algotradingbot.engine;

import java.util.ArrayList;

import com.algotradingbot.chart.CandleChart;
import com.algotradingbot.core.Candle;
import com.algotradingbot.core.StrategyPerformance;
import com.algotradingbot.strategies.BBbandWithComma;
import com.algotradingbot.strategies.MWPatternStrategy;
import com.algotradingbot.utils.CandleUtils;


public class PeriodTesterInteractiveBroker {



    public static void runSinglePeriodTest(String currency, String interval, String duration, String endDateTime,
            String ip, int port) {

        System.out.println("\n============ SINGLE INTERACTIVE BROKER TEST ============\n");
        System.out.printf("Currency: %s | Interval: %s | Duration: %s | Until: %s\n", currency, interval, duration, endDateTime);

        GetDataFromInteractiveBroker fetcher = new GetDataFromInteractiveBroker(
                currency,
                interval,
                duration,
                endDateTime,
                "MIDPOINT", // or "TRADES", "ASK", "BID"
                false, // useRTH
                port,
                ip
        );

        fetcher.connectToInteractiveBroker();

        fetcher.awaitHistoricalData();

        try {

            ArrayList<Candle> candles = CandleUtils.normalizeFxCandles(fetcher.getCandles()); // תוסיף getter

            testMWPatternStrategy(candles);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static StrategyPerformance testMWPatternStrategy(ArrayList<Candle> candles) {
        // בחר גודל סיכון לעסקה ויחס RR (משמש רק לחישוב גודל פוזיציה/דוח)
        double riskPerTradeUSD = 20.0;

        MWPatternStrategy strategy = new MWPatternStrategy(
                candles,
                riskPerTradeUSD
        );

        strategy.runBackTest();
        strategy.evaluateSignals();

        StrategyPerformance perf = strategy.evaluatePerformanceEURUSD();

        CandleChart.showChartFx(
                strategy.getCandles(),
                strategy.getSignals(),
                perf.getCombinedPerformance(),
                CandleChart.ChartOverlayMode.DIVERGENCE
        );

        perf.print();

        return perf;
    }

    private static StrategyPerformance testBBbandWithCommaStrategy(ArrayList<Candle> candles) {
        // בחר גודל סיכון לעסקה ויחס RR (משמש רק לחישוב גודל פוזיציה/דוח)
        double riskPerTradeUSD = 20.0;

        BBbandWithComma strategy = new BBbandWithComma(candles);

        strategy.runBackTest();
        strategy.evaluateSignals();

        StrategyPerformance perf = strategy.evaluatePerformanceEURUSD();

        CandleChart.showChartFx(
                strategy.getCandles(),
                strategy.getSignals(),
                perf.getCombinedPerformance(),
                CandleChart.ChartOverlayMode.DIVERGENCE
        );

        perf.print();

        return perf;
    }
}
