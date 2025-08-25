package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import static com.algotradingbot.core.Candle.isRed;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.FilterRejectionTracker;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

/*
BB band with comma 4h BTCUSDT result
    === Long Performance ===
    W:68  | L:46  | WinRate: 59.65% | Profit: $  993.03 | MaxDD: $   95.46
 * 
 */
public class BBbandWithComma4HBTCUSDT extends TradingStrategy {

    private static final double STRONG_WICK_FACTOR = 1;
    private final int MIN_CANDLES_FOR_STRATEGY = 200;
    private final int COMMA_EMA_PERIOD = 15;
    private static final double BB_PROXIMITY_THRESHOLD = 0.10;

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
                Signal signal = createBuySignalATR(i, curr, atr, ATR_MULT, riskReward);
                //Signal signal = createBuySignalFromClose(i, curr);
                //Signal signal = createBuySignal(i, curr);
                signals.add(signal);
            } else {
                /*  if (strategyValidShort(i)) {
                    Candle curr = candles.get(i);
                    Signal signal = createSellSignalATR(i, curr, atr, ATR_MULT, riskReward);
                    signals.add(signal);
                }*/
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

    private boolean strategyValidShort(int index) {
        Candle cur = candles.get(index);
        Candle prev = candles.get(index - 1);
        tracker.incrementTotal(false);

        // טרנד – אותה בדיקה כמו בלונג
        boolean hasTrend = TrendUtils.isHighTimeFrameCommaForPeriod(candles, index, COMMA_EMA_PERIOD);

        // בולינגר עליון במקום תחתון
        boolean touchesUpperBB = TrendUtils.isTouchingUpperBB(
                candles, index,
                TrendUtils.BBPeriod.BB_22.getPeriod(),
                TrendUtils.BBStdDev.STD_2_0.getMultiplier());

        // נר אדום עם Wick עליון חזק
        boolean strongWick = CandleUtils.isRedWithStrongUpperWick(cur, STRONG_WICK_FACTOR);

        // פילטר זמן
        boolean isTradingDay = !TimeUtils.isSaturday(cur.getDate())
                && !TimeUtils.isSunday(cur.getDate())
                && TimeUtils.isTradingHour(cur.getDate());

        // תבניות נרות – bearish
        boolean isBearishEng = CandleUtils.isBearishEngulfing(prev, cur);
        boolean isRedInsideBar = CandleUtils.isInsideBar(prev, cur)
                && isRed(cur)
                && CandleUtils.hasStrongBody(cur);

        // RSI + קרבה ל־BB עליון
        boolean rsiCloseAndRsiOS = TrendUtils.isNearUpperBB(
                candles, index,
                TrendUtils.BBPeriod.BB_22.getPeriod(),
                BB_PROXIMITY_THRESHOLD,
                TrendUtils.BBStdDev.STD_2_0.getMultiplier())
                && TrendUtils.calculateRSI(
                        candles, index,
                        (int) TrendUtils.RSILevel.RSI_PERIOD_14.getValue())
                >= TrendUtils.RSILevel.OVERSOLD.getValue()
                && isBearishEng;

        // בדיקות
        if (!isTradingDay) {
            tracker.incrementTime(false);
            return false;
        }

        if (!strongWick && !isBearishEng && !isRedInsideBar) {
            tracker.incrementCandle(false);
            return false;
        }

        if (!touchesUpperBB && !rsiCloseAndRsiOS) {
            tracker.incrementBB(false);
            return false;
        }

        if (!hasTrend && !isBearishEng) {
            tracker.incrementTrend(false);
            return false;
        }

        return true;
    }

}
