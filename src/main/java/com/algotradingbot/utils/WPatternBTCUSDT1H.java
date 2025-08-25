package com.algotradingbot.utils;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;

public class WPatternBTCUSDT1H {

    private final ArrayList<Candle> candles;
    private final int currIndex;

    private int candleThatFoundPullBack;
    private final int DIDNT_FOUND_PULL_BACK = -1;
    private final int DIVERGANCE_OFFSET = 5;
    private final int NUM_OF_CANDLES_BELOW_EMA = 50;

    private Candle firstLegCandle; // here: first leg = lowest low candidate

    public WPatternBTCUSDT1H(ArrayList<Candle> candles, int currIndex) {
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
        if (!TrendUtils.isTouchingLowerBB(candles, currIndex, bbPeriod, TrendUtils.BBStdDev.STD_2_0.getMultiplier())) {
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



        for (int i = currIndex; i > currIndex - NUM_OF_CANDLES_BELOW_EMA; i--) {
            Double currEma = TrendUtils.calculateEMAAtIndex(candles, i, TrendUtils.EMAType.EMA_50.getPeriod());
            Candle curr = candles.get(i);
            if (currEma == null || curr.getClose() > currEma ) {
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

            double currRsi = TrendUtils.calculateRSI(candles, i, TrendUtils.RSILevel.RSI_PERIOD_14.getValue());
            rsiMin = Math.min(rsiMin, currRsi);

            if (firstLegCandle == null || firstLegCandle.getLow() >= curr.getLow()) {
                firstLegCandle = curr;
            }
        }

        double rsiLast = TrendUtils.calculateRSI(candles, currIndex, TrendUtils.RSILevel.RSI_PERIOD_14.getValue());

        // Last RSI must be ABOVE the prior RSI minimum (bullish divergence feel)
        if (rsiLast < rsiMin + DIVERGANCE_OFFSET) { // small margin
            return false;
        }

        // Require oversold context on the prior swing: rsiMin ≤ 25
        return firstLegCandle != null && rsiMin <= TrendUtils.RSILevel.OVERSOLD_25.getValue();
    }
}
