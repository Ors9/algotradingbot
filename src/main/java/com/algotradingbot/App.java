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
            USDJPY
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
