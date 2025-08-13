package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.FilterRejectionTracker;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class TrendRSIBBBand extends TradingStrategy {

    private final int MIN_CANDLES_FOR_STRATEGY = 200;
    private final int BOLLINGER_PERIOD = 20;
    private final int RSI_PERIOD = 14;
    private FilterRejectionTracker tracker;

    public TrendRSIBBBand(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 3;
        tracker = new FilterRejectionTracker();
    }

    @Override
    public void runBackTest() {
        for (int i = MIN_CANDLES_FOR_STRATEGY; i < candles.size(); i++) {
            if (strategyValidLong(i)) {
                Candle curr = candles.get(i);
                Signal signal = createBuySignal(i, curr); // שיטה קיימת מה־TradingStrategy
                //Signal signal = createBuySignalATR(i, curr);
                signals.add(signal);
            }
            /*Doesnt Suit for Short!!!!!!! */
 /*if (strategyValidShort(i)) {
                Candle curr = candles.get(i);
                Signal signal = createSellSignal(i, curr); // שיטה קיימת מה־TradingStrategy
                signals.add(signal);
            }*/
        }

        tracker.print();
    }

    private boolean strategyValidLong(int index) {
        tracker.incrementTotal(true);
        Candle curr = candles.get(index);
        Candle prev = candles.get(index - 1);

        if (!TrendUtils.isBullishEnough(candles, index) && !TrendUtils.isShortTermUptrendHolding(candles, index, 20, 50, 200)) {
            tracker.incrementTrend(true);
            return false;
        }

        BollingerBands bb = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD);
        BollingerBands bbBig = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD + 5);

        double touchThresholdSmall = bb.getLower() * 1.03;   // 3% מעל התחתון
        double touchThresholdBig = bbBig.getLower() * 1.05; // 5% מעל התחתון הגדול

        if (curr.getLow() > touchThresholdSmall || curr.getLow() > touchThresholdBig) {
            tracker.incrementBB(true);
            return false;
        }

        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            tracker.incrementTime(true);
            return false;
        }

        double avgRsiLast10 = TrendUtils.averageRSI(candles, index - 11, 11);
        double rsi = TrendUtils.calculateRSI(candles, index, 14);

        double rsiBig = TrendUtils.calculateRSI(candles, index, 21);

        if ((rsi > avgRsiLast10 - 9) && (rsiBig > 40)) {
            tracker.incrementRSI(true);
            return false;
        }

        if (!CandleUtils.hasStrongBody(curr) || !CandleUtils.isGreen(curr)) {
            tracker.incrementCandle(true);
            return false;
        }

        if (!(CandleUtils.isHammer(curr)
                || CandleUtils.isBullishEngulfing(prev, curr)
                || CandleUtils.isTweezerBottom(prev, curr))) {
            tracker.incrementPattern(true);
            return false;
        }

        return true;
    }

    private boolean strategyValidShort(int index) {
        tracker.incrementTotal(false);
        Candle curr = candles.get(index);
        Candle prev = candles.get(index - 1);

        if (!TrendUtils.isBearishEnough(candles, index)) {
            tracker.incrementTrend(false);
            return false;
        }

        // נבדוק פריצה של רצועת עליונה ואז חזרה
        BollingerBands bb = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD);
        double upper = bb.getUpper();

        boolean fakeout = prev.getHigh() > upper && curr.getClose() < upper;
        boolean touchesUpper = curr.getHigh() >= upper * 0.97;

        if (!fakeout && !touchesUpper) {
            tracker.incrementBB(false);
            return false;
        }

        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            tracker.incrementTime(false);
            return false;
        }

        double rsi = TrendUtils.calculateRSI(candles, index, RSI_PERIOD);

        // דרוש RSI גבוה יחסית לשורט (כדי לתפוס תיקון כלפי מטה)
        if (rsi < 60) {
            tracker.incrementRSI(false);
            return false;
        }

        // לוודא שהנר הנוכחי חזק, עם גוף ברור
        if (!CandleUtils.hasStrongBody(curr) || !Candle.isRed(curr)) {
            tracker.incrementCandle(false);
            return false;
        }
        if (CandleUtils.isBullishHarami(prev, curr)) {
            System.out.println("Bullish Harami detected at index " + index + " on " + curr.getDate());
        }

        if (!(CandleUtils.isBearishEngulfing(prev, curr)
                || CandleUtils.isEveningStar(candles.get(index - 2), prev, curr)
                || CandleUtils.isThreeBlackCrows(candles.get(index - 2), prev, curr)
                || CandleUtils.isThreeWhiteSoldiers(candles.get(index - 2), prev, curr))) {

            tracker.incrementPattern(false);
            return false;
        }
        return true;
    }

    private Signal createSellSignal(int index, Candle curr) {
        double entryBuffer = 20;
        double lowPrice = curr.getLow();
        double highPrice = curr.getHigh();

        double entryPrice = lowPrice - entryBuffer;
        double stopLossPrice = highPrice + (highPrice * 0.01);
        double riskPerUnit = stopLossPrice - entryPrice;

        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;
        double takeProfitPrice = entryPrice - (riskReward * riskPerUnit);

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, false);
    }

    private Signal createBuySignalATR(int index, Candle curr) {
        double atr = TrendUtils.calculateATR(candles, index, 14); // או 21 – תבדוק מה נותן תוצאה טובה
        double atrMultiplierSL = 1.5;
        double atrMultiplierTP = 3.0;

        double entryPrice = curr.getHigh() * 1.001; // או close אם זה עדיף
        double stopLossPrice = entryPrice - atr * atrMultiplierSL;
        double takeProfitPrice = entryPrice + atr * atrMultiplierTP;

        double riskPerUnit = entryPrice - stopLossPrice;
        if (riskPerUnit <= 0) {
            return null;
        }

        double positionSize = riskPerTradeUSD / riskPerUnit;

        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, true);
    }

    // --- ATR-based SHORT (entry at close) ---
    public Signal createSellSignalATR(
            int index, Candle curr,
            double atr, double atrMult,
            double riskReward) {

        if (curr == null || atr <= 0 || Double.isNaN(atr) || atrMult <= 0) {
            return null;
        }

        double entry = curr.getClose();                 // ✅ enter at close
        double stop = entry + (atrMult * atr);         // SL above entry by ATR
        double riskPerUnit = stop - entry;              // must be > 0
        if (riskPerUnit <= 0) {
            return null;
        }

        double tp = entry - (riskReward * riskPerUnit); // RR (e.g., 1.0 for 1:1)

        return new Signal(index, entry, tp, stop, riskPerTradeUSD, false);
    }
}
