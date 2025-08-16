package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.MPattern;
import com.algotradingbot.utils.TrendUtils;

public class MWPatternStrategy extends TradingStrategy {


    private static final int START_PERIOD = 70;
    private static final int ATR_PERIOD = 10;
    private static final double RR_RATIO = 0.8;   // 1:1
    private static final double ATR_MULT = 2.0;   // כמה ATR לשים לסטופ
   

    public MWPatternStrategy(ArrayList<Candle> candles, double riskPerTradeUSD) {
        super(candles);
        this.riskPerTradeUSD = riskPerTradeUSD;
    }

    @Override
    public void runBackTest() {
        if (candles == null || candles.size() < START_PERIOD) {
            return;
        }

        for (int i = START_PERIOD; i < candles.size(); i++) {
            if (checkForM(i)) {
                double atr = TrendUtils.calculateATR(candles, i, ATR_PERIOD);
                if (!Double.isNaN(atr) && atr > 0) {
                    Candle curr = candles.get(i);
        
                    Signal sig = createSellSignalATR_EURUSD(i, curr, atr, ATR_MULT, RR_RATIO);
                    if (sig != null) {
                        signals.add(sig);
                    }
                }
            }

            // בעתיד: checkForW(i) -> לבנות LONG באותה לוגיקה (סטופ מתחת ל-entry)
        }
    }

    private boolean checkForM(int currIndex) {
        MPattern mp = new MPattern(candles, currIndex);
        return mp.analyzeMPattern();
    }

}
