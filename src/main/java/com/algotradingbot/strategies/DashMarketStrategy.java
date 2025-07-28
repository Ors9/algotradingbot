package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class DashMarketStrategy extends TradingStrategy {

    private final int MIN_CANDLES_FOR_STRATEGY = 300;
    private final int BOLLINGER_PERIOD = 20;
    private final int RSI_PERIOD = 14;

    public DashMarketStrategy(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 5;
    }

    @Override
    public void runBackTest() {
        for (int i = MIN_CANDLES_FOR_STRATEGY; i < candles.size(); i++) {
            if (strategyValid(i)) {
                Candle curr = candles.get(i);
                Signal signal = createBuySignal(i, curr); // שיטה קיימת מה־TradingStrategy
                signals.add(signal);
            }
        }
    }

    private boolean strategyValid(int index) {
        Candle curr = candles.get(index);
        Candle prev = candles.get(index - 1);

        // מגמה פשוטה בלבד
        /*if (!TrendUtils.isShortTermUptrendHolding(candles, index, 20, 50, 200)) {
            return false;
        }*/

        // BB pullback zone
        BollingerBands bb = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD);
        double distanceFromLower = (curr.getClose() - bb.lower) / (bb.upper - bb.lower);
        if (distanceFromLower > 0.7) {
            return false;
        }

        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            return false;
        }


        if (!CandleUtils.hasStrongBody(curr)) {
            return false;
        }

        if (!CandleUtils.isGreen(curr)) {
            return false;
        }

        // RSI גמיש יותר
        double rsi = TrendUtils.calculateRSI(candles, index, 14);
        if (rsi > 60 || rsi < 20) {
            return false;
        }

        // תבניות נר – רק אם ממש חשוב
        if (!CandleUtils.isHammer(curr)
                && !CandleUtils.isBullishEngulfing(prev, curr)) {
            return false;
        }

        return true;
    }

    private Signal createBuySignal(int index, Candle curr) {
        double bufferPercent = 0.01; // 1%
        double high = curr.getHigh();
        double low = curr.getLow();

        double entry = high * (1 + bufferPercent);      // כניסה 1% מעל השיא
        double sl = low * (1 - bufferPercent);          // סטופ 1% מתחת לשפל
        double riskPerUnit = entry - sl;

        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double tp = entry + (riskReward * riskPerUnit);

        return new Signal(index, entry, tp, sl, positionSize);
    }
}
