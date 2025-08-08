package com.algotradingbot.engine;

import java.util.ArrayList;

import com.algotradingbot.chart.CandleChart;
import com.algotradingbot.core.Candle;
import com.algotradingbot.core.StrategyPerformance;
import com.algotradingbot.strategies.OldInsideBarStrategy;

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

        try {


            ArrayList<Candle> candles = fetcher.getCandles(); // תוסיף getter
            if (candles.isEmpty()) {
                System.err.println("❌ No candles received — chart cannot be created.");
                return;
            }
            // הרץ אסטרטגיה
            OldInsideBarStrategy strategy = new OldInsideBarStrategy(candles);
            strategy.runBackTest();
            strategy.evaluateSignals();
            StrategyPerformance perf = strategy.evaluatePerformance();
            perf.print();

            CandleChart.showChart(strategy.getCandles(), strategy.getSignals(), perf.getCombinedPerformance());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
