package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.BollingerBands.BBPeriod;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class ExecuteDemoBBbandWithComma extends TradingStrategy {
    private RoollingWindow roollingWindow;
    public ExecuteDemoBBbandWithComma(ArrayList<Candle> candles) {
        super(null);
        roollingWindow = new RoollingWindow();
    }

    // ==== Additions for live/demo execution (evaluate only the last bar) ====
    private static final double STRONG_WICK_FACTOR = 1.6;
    private final int MIN_CANDLES_FOR_STRATEGY = 200;
    private final int COMMA_EMA_PERIOD = 15;

    @Override
    public void runBackTest() {

        throw new UnsupportedOperationException("Unsupport at demo trading");
    }

    public Signal evaluateLastOn(java.util.List<Candle> window) {
        int n = window.size();
        if (n < MIN_CANDLES_FOR_STRATEGY) {
            return null;
        }
        int i = n - 1;

        // נריץ את אותם חוקים על ה-window שנמסר (לא על this.candles)
        if (strategyValidLong(i, window)) {
            Candle curr = window.get(i);
            return createBuySignalFromClose(i, curr);
        }
        if (strategyValidShort(i, window)) {
            Candle curr = window.get(i);
            return createSellSignalFromClose(i, curr);
        }
        return null;
    }

// גרסאות Overload שעובדות על window חיצוני:
    private boolean strategyValidLong(int index, java.util.List<Candle> cs) {
        Candle cur = cs.get(index);
        Candle prev = cs.get(index - 1);
        // לא נוגעים ב-tracker פה כדי לא לזהם סטטיסטיקה של backtest
        boolean hasTrend = TrendUtils.isHighTimeFrameCommaForPeriod(cs, index, COMMA_EMA_PERIOD);
        boolean touchesLowerBB = TrendUtils.isTouchingLowerBB(cs, index, BBPeriod.BB_22.getPeriod());
        boolean strongWick = CandleUtils.isGreenWithStrongLowerWick(cur, STRONG_WICK_FACTOR);
        boolean isTradingDay = !TimeUtils.isSaturday(cur.getDate()) && !TimeUtils.isSunday(cur.getDate());
        boolean isBullishEng = CandleUtils.isBullishEngulfing(prev, cur);

        if (!isTradingDay) {
            return false;
        }
        if (!strongWick && !isBullishEng) {
            return false;
        }
        if (!touchesLowerBB) {
            return false;
        }
        if (!hasTrend && !isBullishEng) {
            return false;
        }
        return true;
    }

    private boolean strategyValidShort(int index, java.util.List<Candle> cs) {
        Candle cur = cs.get(index);
        Candle prev = cs.get(index - 1);
        boolean hasTrend = TrendUtils.isHighTimeFrameBearishComma(cs, index, COMMA_EMA_PERIOD);
        boolean touchesUpperBB = TrendUtils.isTouchingUpperBB(cs, index, BBPeriod.BB_20.getPeriod());
        boolean weakWick = CandleUtils.isRedWithStrongUpperWick(cur, STRONG_WICK_FACTOR);
        boolean isTradingDay = !TimeUtils.isSaturday(cur.getDate()) && !TimeUtils.isSunday(cur.getDate());
        boolean isBearishEng = CandleUtils.isBearishEngulfing(prev, cur);
        boolean isBigRedInsideBar
                = CandleUtils.isInsideBar(prev, cur)
                && Candle.isRed(cur)
                && CandleUtils.hasStrongBody(cur);

      

        if (!isTradingDay) {
            return false;
        }
        if (!isBearishEng && !isBigRedInsideBar && !weakWick) {
            return false; // מעט ריכוך כדי לא לפספס

        }
        if (!touchesUpperBB ) {
            return false;
        }
        if (!hasTrend) {
            return false;
        }
        return true;
    }

}
