package com.algotradingbot;

import com.algotradingbot.engine.PeriodTesterBinance;
import com.algotradingbot.engine.PeriodTesterInteractiveBroker;

public class App {

    public static void main(String[] args) {
        //BackTesterBinance();
        BackTesterInteractiveBroker();

    }

    public static void executeDemoTrading() {

    }

    public static void BackTesterBinance() {

        //PeriodTesterBinance.runTestsForMultiplePeriods("BTCUSDT", "4h");
        long start = 1483228800000L; // 2017-01-01 00:00:00 UTC
        long end = System.currentTimeMillis(); // עכשיו
        PeriodTesterBinance.runSinglePeriodTestBinance("BTCUSDT", "4h", start, end); //almost 9 years

        //PeriodTesterBinance.runSinglePeriodTestBinance("BTCUSDT", "15m", start, end); //almost 9 years
        //BTCUSDT  ETHUSDT  BNBUSDT   XRPUSDT SOLUSDT
    }

    public static void BackTesterInteractiveBroker() {

        /*
        =========================================
        📌 זוגות מט"ח זמינים בד"כ ב-IBKR Demo
        (ללא חבילת נתונים בתשלום)
        =========================================

        Majors:
            EURUSD
            USDJPY-> dont valid with my calculate of stop and target
            GBPUSD
            USDCHF
            AUDUSD
            USDCAD
            NZDUSD
         */
        PeriodTesterInteractiveBroker.runSinglePeriodTest(
                "EURUSD", // או כל זוג נתמך
                "4 hours",
                "10 Y",
                "",
                "127.0.0.1",
                7497
        );

    }

}

/*
    (Not good enough the 4 hour!!!!)
 * MW Pattern 4H EurUsd Result: 
 * === Long Performance ===
    W:25  | L:12  | WinRate: 67.57% | Profit: $  123.00 | MaxDD: $   84.00
    === Short Performance ===
    W:30  | L:2   | WinRate: 93.75% | Profit: $  408.00 | MaxDD: $   42.00
    === Combined Performance ===
    W:55  | L:14  | WinRate: 79.71% | Profit: $  531.00 | MaxDD: $   84.00
 * 
 */
 /*
 * MW Pattern 1H EurUsd RESULT: 
    * === Long Performance ===
    W:131 | L:67  | WinRate: 66.16% | Profit: $  558.00 | MaxDD: $  237.00
    === Short Performance ===
    W:96  | L:42  | WinRate: 69.57% | Profit: $  558.00 | MaxDD: $  261.00
    === Combined Performance ===
    W:227 | L:109 | WinRate: 67.56% | Profit: $ 1116.00 | MaxDD: $  261.00
 * 
 */
