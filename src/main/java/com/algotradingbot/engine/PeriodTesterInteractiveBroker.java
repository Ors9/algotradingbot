package com.algotradingbot.engine;

import java.util.ArrayList;

import com.algotradingbot.chart.CandleChart;
import com.algotradingbot.core.Candle;
import com.algotradingbot.core.StrategyPerformance;
import com.algotradingbot.strategies.BBbandWithComma;
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
            if (candles.isEmpty()) {
                System.err.println("❌ No candles received — chart cannot be created.");
                return;
            }

            System.out.println("Candles received: " + candles.size());
            System.out.println("First: " + candles.get(0).getDate() + "  Last: " + candles.get(candles.size() - 1).getDate());

            // הרץ אסטרטגיה
            BBbandWithComma strategy = new BBbandWithComma(candles);
            strategy.runBackTest();
            strategy.evaluateSignals();
            StrategyPerformance perf = strategy.evaluatePerformance();
            perf.print();

            CandleChart.showChartFx(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance() , CandleChart.ChartOverlayMode.DIVERGENCE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
