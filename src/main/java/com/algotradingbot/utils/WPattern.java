package com.algotradingbot.utils;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;

/*
 * W Pattern tested on 4H EURUSD (analog of M Pattern)
 *
 *  1. Last candle must touch the LOWER Bollinger Band (20, 2.0).
 *  2. Last candle must be the LOWEST LOW within the lookback window (50 bars).
 *  3. At least one candle before must CLOSE ABOVE SMA20 (pullback required).
 *  4. The last 70 candles must all CLOSE BELOW EMA50 (exhausted bearish condition).
 *  5. RSI MIN (period 10) between pullback and last candle must be recorded.
 *  6. Last candle’s RSI must be HIGHER than the previous RSI MIN.
 *  7. The RSI MIN must be ≤ 25 (oversold).
 *
 *  Notes:
 *  - Structure, fields, and control flow mirror MPattern for symmetry.
 *  - Indices and window logic kept identical for consistency.
 */
public class WPattern {

    private final ArrayList<Candle> candles;
    private final int currIndex;

    private int candleThatFoundPullBack;
    private final int DIDNT_FOUND_PULL_BACK = -1;

    private Candle firstLegCandle; // here: first leg = lowest low candidate

    public WPattern(ArrayList<Candle> candles, int currIndex) {
        this.candles = candles;
        this.currIndex = currIndex;
        this.firstLegCandle = null;
    }

    public boolean analyzeWPattern() {
        if (candles == null || candles.isEmpty()) {
            return false;
        }

        int bbPeriod = TrendUtils.BBPeriod.BB_20.getPeriod();
        Candle lastCandle = candles.get(currIndex);

        // (0) L2 must touch Lower BB (20, 2.0)
        if (!TrendUtils.isTouchingLowerBB(
                candles,
                currIndex,
                bbPeriod,
                TrendUtils.BBStdDev.STD_2_0.getMultiplier())) {
            return false;
        }

        int patternSizeMax = 50;
        candleThatFoundPullBack = DIDNT_FOUND_PULL_BACK;
        firstLegCandle = null;

        // Scan back to ensure last candle is the LOWEST low in the window,
        // and look for a pullback ABOVE SMA20 at least once before it.
        for (int i = currIndex - 1; i > currIndex - patternSizeMax; i--) {
            Candle curr = candles.get(i);

            // last candle must remain the LOWEST low in the window
            if (lastCandle.getLow() >= curr.getLow()) {
                return false;
            }

            // Pullback requirement: at least one close ABOVE SMA20
            try {
                double sma20 = TrendUtils.calculateSMA(candles, i, TrendUtils.SMAType.SMA_20.getPeriod());
                if (curr.getClose() >= sma20) {
                    candleThatFoundPullBack = i;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (candleThatFoundPullBack == DIDNT_FOUND_PULL_BACK) {
            return false;
        }

        // Exhausted bearish: last N candles all close BELOW EMA50
        int numberOfCandlesBelowEma = 70;
        for (int i = currIndex; i > currIndex - numberOfCandlesBelowEma; i--) {
            Double currEma = TrendUtils.calculateEMAAtIndex(candles, i, TrendUtils.EMAType.EMA_50.getPeriod());
            Candle curr = candles.get(i);
            if (currEma == null || curr.getClose() > currEma) {
                return false;
            }
        }

        // Track RSI MIN between pullback (exclusive) and window start,
        // while preserving the "last candle is lowest low" invariant
        double rsiMin = Double.POSITIVE_INFINITY;
        for (int i = candleThatFoundPullBack - 1; i > currIndex - patternSizeMax; i--) {
            Candle curr = candles.get(i);

            // Maintain lowest-low requirement
            if (lastCandle.getLow() >= curr.getLow()) {
                return false;
            }

            double currRsi = TrendUtils.calculateRSI(candles, i, TrendUtils.RSILevel.RSI_PERIOD_10.getValue());
            rsiMin = Math.min(rsiMin, currRsi);

            if (firstLegCandle == null || firstLegCandle.getLow() >= curr.getLow()) {
                firstLegCandle = curr;
            }
        }

        double rsiLast = TrendUtils.calculateRSI(candles, currIndex, TrendUtils.RSILevel.RSI_PERIOD_10.getValue());

        // Last RSI must be ABOVE the prior RSI minimum (bullish divergence feel)
        if (rsiMin > rsiLast) {
            return false;
        }

        // Require oversold context on the prior swing: rsiMin ≤ 25
        return firstLegCandle != null && rsiMin <= TrendUtils.RSILevel.OVERSOLD_25.getValue();
    }
}
