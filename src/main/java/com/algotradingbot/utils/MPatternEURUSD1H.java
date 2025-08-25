package com.algotradingbot.utils;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;


 /*
 
 * MW Pattern 1H EURUSD RESULT: 
=== Long Performance ===
W:125 | L:65  | WinRate: 65.79% | Profit: $  510.00 | MaxDD: $  201.00
=== Short Performance ===
W:112 | L:61  | WinRate: 64.74% | Profit: $  399.00 | MaxDD: $  150.00
=== Combined Performance ===
W:237 | L:126 | WinRate: 65.29% | Profit: $  909.00 | MaxDD: $  201.00
 * 
 */


/*
 * M Pattern tested on 1H EURUSD

    1. Last candle must touch the upper Bollinger Band (20, 2.0).
    2. Last candle must be the highest high within the lookback window (50 bars).
    3. At least one candle before must close below SMA20 (pullback required).
    4. The last 70 candles must all close above EMA50 (exhausted market condition).
    5. RSI max (period 10) between pullback and last candle must be recorded.
    6. Last candle’s RSI must be lower than the previous RSI max.
    7. The RSI max must be ≥ 75 (overbought).

 * 
 */
public class MPatternEURUSD1H {

    private final ArrayList<Candle> candles;
    private final int currIndex;

    private int candleThatFoundPullBack;
    private final int DIDNT_FOUND_PULL_BACK = -1;

    private Candle firstLegCandle;

    public MPatternEURUSD1H(ArrayList<Candle> candles, int currIndex) {
        this.candles = candles;
        this.currIndex = currIndex;
        this.firstLegCandle = null;
    }

    public boolean analyzeMPattern() {
        if (candles == null || candles.isEmpty()) {
            return false;
        }

        int bbPeriod = TrendUtils.BBPeriod.BB_20.getPeriod();

        Candle lastCandle = candles.get(currIndex);

        // (0) H2 חייב לגעת Upper BB
        if (!TrendUtils.isTouchingUpperBB(candles, currIndex, bbPeriod, TrendUtils.BBStdDev.STD_2_0.getMultiplier())) {
            return false;
        }

        int patternSizeMax = 50;
        candleThatFoundPullBack = DIDNT_FOUND_PULL_BACK;
        firstLegCandle = null;

        for (int i = currIndex - 1; i > currIndex - patternSizeMax; i--) {
            Candle curr = candles.get(i);
            /*because we want the last candle to be the highest at any moment at the windows!*/
            if (lastCandle.getHigh() <= curr.getHigh()) {
                return false;
            }

            /*now i want pull back to sma so i want at least 1 candle to close below SMA20*/
            try {
                double sma20 = TrendUtils.calculateSMA(candles, i, TrendUtils.SMAType.SMA_20.getPeriod());
                if (curr.getClose() <= sma20) {
                    candleThatFoundPullBack = i;
                    break;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        if (candleThatFoundPullBack == DIDNT_FOUND_PULL_BACK) {
            return false;
        }

        //Try looking for Exausted market all 70 candles close  above the 50 ema
        int numberOfCandlesAboveEma = 50;
        for (int i = currIndex; i > currIndex - numberOfCandlesAboveEma; i--) {
            Double currEma = TrendUtils.calculateEMAAtIndex(candles, i, TrendUtils.EMAType.EMA_50.getPeriod());
            Candle curr = candles.get(i);
            if (currEma == null || curr.getClose() < currEma) {
                return false;
            }
        }

        double rsiMax = -1;
        for (int i = candleThatFoundPullBack - 1; i > currIndex - patternSizeMax; i--) {
            Candle curr = candles.get(i);
            /*because we want the last candle to be the highest at any moment at the windows!*/
            if (lastCandle.getHigh() <= curr.getHigh()) {
                return false;
            }
            double currRsi = TrendUtils.calculateRSI(candles, i, TrendUtils.RSILevel.RSI_PERIOD_10.getValue());
            rsiMax = Math.max(rsiMax, currRsi);

            if (firstLegCandle == null || firstLegCandle.getHigh() <= curr.getHigh()) {
                firstLegCandle = curr;
            }

        }

        double rsiLast = TrendUtils.calculateRSI(candles, currIndex, TrendUtils.RSILevel.RSI_PERIOD_10.getValue());

        if (rsiMax < rsiLast) {
            return false;
        }

        return firstLegCandle != null && rsiMax >= TrendUtils.RSILevel.OVERBOUGHT_75.getValue();
    }

}
