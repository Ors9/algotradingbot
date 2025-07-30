package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class OldInsideBarStrategy extends TradingStrategy {

    private final int SMA_DAYS_20 = 20;
    private final int SMA_DAYS_50 = 50;
    private final int SMA_DAYS_200 = 200;

    public OldInsideBarStrategy(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 5.0;
    }

    @Override
    public void runBackTest() {
        for (int i = SMA_DAYS_50 + 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);

            if (strategyInsideBar(prev, curr, i)) {
                signals.add(createBuySignal(i, curr));
            }
        }
    }

    private Signal createBuySignal(int index, Candle curr) {
        double entry = curr.getHigh() + 20;  // כניסה 20 דולר מעל ה-high
        double sl = curr.getLow() - 20;      // SL 20 דולר מתחת ל-low
        double riskPerUnit = entry - sl;

        if (riskPerUnit <= 0) {
            return null; // מקרה קצה – לא סביר
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double tp = entry + (riskReward * riskPerUnit);

        return new Signal(index, entry, tp, sl, positionSize , true);
    }

    public boolean strategyInsideBar(Candle prev, Candle curr, int index) {
        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            return false;
        }

        if (!CandleUtils.isGreen(curr) || !CandleUtils.hasStrongBody(curr)) {
            return false;
        }

        if (!CandleUtils.isInsideBar(prev, curr)) {
            return false;
        }

        if (!TrendUtils.isStrongBullMarket(candles, index, SMA_DAYS_20, SMA_DAYS_50, SMA_DAYS_200)) {
            return false;
        }

        if (!TrendUtils.isHighTFBullTrend(curr, "BTCUSDT", SMA_DAYS_20, SMA_DAYS_50, SMA_DAYS_200)) {
            return false;
        }

        return true;
    }

}
