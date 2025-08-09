package com.algotradingbot;

import com.algotradingbot.engine.PeriodTesterBinance;

public class App {

    public static void main(String[] args) {
        //PeriodTesterBinance.runTestsForMultiplePeriods("BTCUSDT", "15m");
        //PeriodTesterBinance.runTestsForMultiplePeriods("BTCUSDT", "4h");
        long start = 1483228800000L; // 2017-01-01 00:00:00 UTC
        long end = System.currentTimeMillis(); // עכשיו
        PeriodTesterBinance.runSinglePeriodTestBinance("BTCUSDT", "4h", start, end); //almost 9 years
 
        //PeriodTesterBinance.runSinglePeriodTestBinance("BTCUSDT", "15m", start, end); //almost 9 years
        /*PeriodTesterInteractiveBroker.runSinglePeriodTest(
                "EUR", // currency
                "1 hour", // interval
                "1 D", // duration
                "", // endDateTime ("" = now)
                "127.0.0.1", // IP
                7497 // port (TWS default)
        );*/
        //BTCUSDT  ETHUSDT  BNBUSDT   XRPUSDT SOLUSDT
    }

}
