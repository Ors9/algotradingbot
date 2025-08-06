package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.FilterRejectionTracker;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class OldInsideBarStrategy extends TradingStrategy {

    private final int SMA_DAYS_20 = 20;
    private final int SMA_DAYS_50 = 50;
    private final int SMA_DAYS_200 = 200;
    private FilterRejectionTracker tracker;

    public OldInsideBarStrategy(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 1.0;
        tracker = new FilterRejectionTracker();
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
    /* 
    private Signal createBuySignal(int index, Candle curr) {
        double entryBufferPct = 0.002; // 0.2% מעל השיא
        double stopLossPct = 0.01;     // 1% מתחת לשפל

        double highPrice = curr.getHigh();
        double lowPrice = curr.getLow();

        double entryPrice = highPrice * (1 + entryBufferPct);
        double stopLossPrice = lowPrice * (1 - stopLossPct);

        double riskPerUnit = entryPrice - stopLossPrice;

        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double takeProfitPrice = entryPrice + (riskReward * riskPerUnit);

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, true);
    }*/

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
        tracker.incrementTotal(true);
        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            tracker.incrementTime(true);
            return false;
        }

        if (!CandleUtils.isGreen(curr) || !CandleUtils.hasStrongBody(curr)) {
            tracker.incrementPattern(true);
            return false;
        }

        if (!CandleUtils.isInsideBar(prev, curr)) {
            tracker.incrementCandle(true);
            return false;
        }

        if (!TrendUtils.isStrongBullMarket(candles, index, SMA_DAYS_20, SMA_DAYS_50, SMA_DAYS_200)) {
            tracker.incrementTrend(true);
            return false;
        }

        if (!TrendUtils.isHighTFBullTrend(curr, "BTCUSDT", SMA_DAYS_20, SMA_DAYS_50, SMA_DAYS_200)) {
            tracker.incrementTrend(true);
            return false;
        }


        return true;
    }

}
