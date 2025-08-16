package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.MPattern;
import com.algotradingbot.utils.TrendUtils;
import com.algotradingbot.utils.WPattern;

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
            double atr = TrendUtils.calculateATR(candles, i, ATR_PERIOD);
            if (Double.isNaN(atr) || atr <= 0) {
                continue;
            }

            Candle curr = candles.get(i);

            // 1) M-pattern -> SHORT
            if (checkForM(i)) {
                Signal shortSig = createSellSignalATR_MajorForex(i, curr, atr, ATR_MULT, RR_RATIO);
                if (shortSig != null) {
                    signals.add(shortSig);
                }
                continue; // avoid double-signalling on the same bar
            }

            // 2) W-pattern -> LONG
            if (checkForW(i)) {
                // Use the buy-side ATR helper parallel to your sell helper.
                // If your base class uses a different name, swap it accordingly.
                Signal longSig = createBuySignalATR_MajorForex(i, curr, atr, ATR_MULT, RR_RATIO);
                if (longSig != null) {
                    signals.add(longSig);
                }
            }
        }
    }

    private boolean checkForM(int currIndex) {
        return new MPattern(candles, currIndex).analyzeMPattern();
    }

    private boolean checkForW(int currIndex) {
        return new WPattern(candles, currIndex).analyzeWPattern();
    }

}
