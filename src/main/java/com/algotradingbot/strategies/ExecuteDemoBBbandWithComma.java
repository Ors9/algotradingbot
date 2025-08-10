package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;

public class ExecuteDemoBBbandWithComma extends TradingStrategy {
    //private RoollingWindow roollingWindow;
    public ExecuteDemoBBbandWithComma(ArrayList<Candle> candles) {
        super(null);
        //roollingWindow = new RoollingWindow();
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
        return false;
    }

    private boolean strategyValidShort(int index, java.util.List<Candle> cs) {
 
        return false;
    }

}
