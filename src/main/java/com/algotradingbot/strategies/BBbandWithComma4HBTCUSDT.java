package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.FilterRejectionTracker;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

/*
BB band with comma 4h BTCUSDT result
    === Long Performance ===
    W:69  | L:54  | WinRate: 56.10% | Profit: $  855.88 | MaxDD: $  109.41
 * 
 */
public class BBbandWithComma4HBTCUSDT extends TradingStrategy {

    private static final double STRONG_WICK_FACTOR = 1.6;
    private final int MIN_CANDLES_FOR_STRATEGY = 200;
    private final int COMMA_EMA_PERIOD = 15;
    private static final double BB_PROXIMITY_THRESHOLD = 0.15; // 15% מהטווח לבולינגר התחתון

    private static final int ATR_PERIOD = 10;
    private static final double ATR_MULT = 2;  

    private FilterRejectionTracker tracker;

    /* BTCUSDT 4h time frame
     * 1. COMMA 4ema expontial 
     * 2. bb touch low.
     * 3. reverse candle
     * 4. entry stop loss
     */
    public BBbandWithComma4HBTCUSDT(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 1.5;
        tracker = new FilterRejectionTracker();
    }

    @Override
    public void runBackTest() {

        for (int i = MIN_CANDLES_FOR_STRATEGY; i < candles.size(); i++) {
            double atr = TrendUtils.calculateATR(candles, i, ATR_PERIOD);
            if (Double.isNaN(atr) || atr <= 0) {
                continue;
            }

            if (strategyValidLong(i)) {
                Candle curr = candles.get(i);
                Signal signal  = createBuySignalATR(i, curr, atr, ATR_MULT, riskReward);
                //Signal signal = createBuySignalFromClose(i, curr);
                //Signal signal = createBuySignal(i, curr);
                signals.add(signal);
            }

        }

        tracker.print();
    }

    private boolean strategyValidLong(int index) {
        Candle cur = candles.get(index);
        Candle prev = candles.get(index - 1);
        tracker.incrementTotal(true);
        //boolean hasTrend = TrendUtils.isHighTimeFrameComma(candles, index);

        boolean hasTrend = TrendUtils.isHighTimeFrameCommaForPeriod(candles, index, COMMA_EMA_PERIOD);

        boolean touchesLowerBB = TrendUtils.isTouchingLowerBB(candles, index, TrendUtils.BBPeriod.BB_22.getPeriod(), TrendUtils.BBStdDev.STD_2_0.getMultiplier());
        boolean strongWick = CandleUtils.isGreenWithStrongLowerWick(cur, STRONG_WICK_FACTOR);
        boolean isTradingDay = !TimeUtils.isSaturday(cur.getDate()) && !TimeUtils.isSunday(cur.getDate()) && TimeUtils.isTradingHour(cur.getDate());
        boolean isBullishEng = CandleUtils.isBullishEngulfing(prev, cur);
        boolean isGreenInsideBar = CandleUtils.isInsideBar(prev, cur) && CandleUtils.isGreen(cur) && CandleUtils.hasStrongBody(cur);
        boolean rsiCloseAndRsiOB = TrendUtils.isNearLowerBB(candles, index, TrendUtils.BBPeriod.BB_22.getPeriod(), BB_PROXIMITY_THRESHOLD, TrendUtils.BBStdDev.STD_2_0.getMultiplier())
                && TrendUtils.calculateRSI(candles, index, (int) TrendUtils.RSILevel.RSI_PERIOD_14.getValue()) <= TrendUtils.RSILevel.OVERBOUGHT.getValue() && isBullishEng;

        if (!isTradingDay) {
            tracker.incrementTime(true);
            return false;
        }

        if (!strongWick && !isBullishEng && !isGreenInsideBar) {
            tracker.incrementCandle(true);
            return false;
        }

        if (!touchesLowerBB && !rsiCloseAndRsiOB) {
            tracker.incrementBB(true);
            return false;
        }

        if (!hasTrend && !isBullishEng) {
            tracker.incrementTrend(true);
            return false;
        }

        return true;
    }

}
