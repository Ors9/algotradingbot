package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.BollingerBands.BBPeriod;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.FilterRejectionTracker;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class BBbandWithComma extends TradingStrategy {

    private final int MIN_CANDLES_FOR_STRATEGY = 200;
    private final int COMMA_EMA_PERIOD = 10;

    private FilterRejectionTracker tracker;

    /* BTCUSDT 4h time frame
     * 1. COMMA 4ema expontial 
     * 2. bb touch low.
     * 3. reverse candle
     * 4. entry stop loss
     */
    public BBbandWithComma(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 1;
        tracker = new FilterRejectionTracker();
    }

    @Override
    public void runBackTest() {

        for (int i = MIN_CANDLES_FOR_STRATEGY; i < candles.size(); i++) {
            if (strategyValidLong(i)) {
                Candle curr = candles.get(i);
                Signal signal = createBuySignal(i, curr);

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

        boolean touchesLowerBB = TrendUtils.isTouchingLowerBB(candles, index, BBPeriod.BB_20.getPeriod());
        boolean approvedCandle = CandleUtils.isHammer(cur)
                || CandleUtils.isInvertedHammer(cur)
                || CandleUtils.isBullishEngulfing(prev, cur)
                || CandleUtils.isPiercingLine(prev, cur)
                || CandleUtils.isTweezerBottom(prev, cur)
                || CandleUtils.isMorningStar(candles.get(index - 2), prev, cur);

        boolean isTradingDay = !TimeUtils.isSaturday(cur.getDate()) && !TimeUtils.isSunday(cur.getDate());

        if (!isTradingDay) {
            tracker.incrementTime(true);
            return false;
        }

        if (!hasTrend) {
            tracker.incrementTrend(true);
            return false;
        }

        if (!touchesLowerBB) {
            tracker.incrementBB(true);
            return false;
        }
        //!CandleUtils.isGreen(cur)) isStrongBody לחשוב אם לשלב אותם
        if (!approvedCandle) {
            tracker.incrementCandle(true);
            return false;
        }
        return true;
    }

}
