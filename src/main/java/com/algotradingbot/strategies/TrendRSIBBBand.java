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

/*
 * Period: 01/01/2021 ? 01/01/2025
 * Results for Strategy:
W:20  | L:13  | WinRate: 60.61% | Profit: $  869.25 | MaxDD: $   44.92
last result
 * 
 */
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
                signals.add(signal);
            }
            if (strategyValidShort(i)) {
                Candle curr = candles.get(i);
                Signal signal = createSellSignal(i, curr); // שיטה קיימת מה־TradingStrategy
                signals.add(signal);
            }
        }

        tracker.print();
    }

    private boolean strategyValidLong(int index) {
        tracker.incrementTotal(true);
        Candle curr = candles.get(index);
        Candle prev = candles.get(index - 1);

        if (!TrendUtils.isBullishEnough(candles, index)) {
            tracker.incrementTrend(true);
            return false;
        }

        BollingerBands bb = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD);
        BollingerBands bbBig = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD + 5);

        double touchThresholdSmall = bb.lower * 1.03;   // 3% מעל התחתון
        double touchThresholdBig = bbBig.lower * 1.05; // 5% מעל התחתון הגדול

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

        if (!(CandleUtils.isHammer(curr) || CandleUtils.isBullishEngulfing(prev, curr))) {
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

        BollingerBands bb = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD);
        BollingerBands bbBig = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD + 5);

        double touchThresholdSmall = bb.upper * 0.97;   // 3% מתחת לעליונה
        double touchThresholdBig = bbBig.upper * 0.95;  // 5% מתחת לעליונה הגדולה

        // אם הנר הנוכחי לא נוגע ברצועות העליונות (או לא קרוב), נפסול
        if (curr.getHigh() < touchThresholdSmall || curr.getHigh() < touchThresholdBig) {
            tracker.incrementBB(false);
            return false;
        }

        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            tracker.incrementTime(false);
            return false;
        }

        double avgRsiLast10 = TrendUtils.averageRSI(candles, index - 11, 11);
        double rsi = TrendUtils.calculateRSI(candles, index, 14);
        double rsiBig = TrendUtils.calculateRSI(candles, index, 21);

        // נרצה ש־RSI יהיה גבוה כדי לחפש תיקון כלפי מטה
        if ((rsi < avgRsiLast10 + 9) && (rsiBig < 70)) {
            tracker.incrementRSI(false);
            return false;
        }

        if (!CandleUtils.hasStrongBody(curr) || !Candle.isRed(curr)) {
            tracker.incrementCandle(false);
            return false;
        }

        if (!(CandleUtils.isShootingStar(curr) || CandleUtils.isBearishEngulfing(prev, curr))) {
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

    private Signal createBuySignal(int index, Candle curr) {
        // קבוע המייצג מרחק כניסה מעל השיא (מחיר במטבע)
        double entryBuffer = 20;

        // מחירי השיא והשפל של הנר הנוכחי
        double highPrice = curr.getHigh();
        double lowPrice = curr.getLow();

        // מחיר כניסה - 20 יחידות מעל השיא
        double entryPrice = highPrice + entryBuffer;

        // מחיר סטופ לוס - 1% מתחת לשפל
        double stopLossPrice = lowPrice - (lowPrice * 0.01);

        // חישוב הסיכון ליחידה (מרחק בין כניסה לסטופ)
        double riskPerUnit = entryPrice - stopLossPrice;

        // אם הסיכון לא חיובי, אין טעם ליצור סיגנל
        if (riskPerUnit <= 0) {
            return null;
        }

        // גודל הפוזיציה בחשבון (20 דולר סיכון מקסימלי)
        double positionSize = riskPerTradeUSD / riskPerUnit;

        // מחיר היעד (Take Profit) לפי יחס סיכון-רווח
        double takeProfitPrice = entryPrice + (riskReward * riskPerUnit);

        // מחזיר את אובייקט הסיגנל עם כל הנתונים
        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize, true);
    }
}
