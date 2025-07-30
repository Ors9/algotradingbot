package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.CandleUtils;
import com.algotradingbot.utils.TimeUtils;
import com.algotradingbot.utils.TrendUtils;

public class TrendRSIBBBand extends TradingStrategy {

    private final int MIN_CANDLES_FOR_STRATEGY = 200;
    private final int BOLLINGER_PERIOD = 20;
    private final int RSI_PERIOD = 14;

    private int countInvalidTrend = 0;
    private int countInvalidBB = 0;
    private int countInvalidTime = 0;
    private int countInvalidRSI = 0;
    private int countInvalidBodyOrColor = 0;
    private int countInvalidPattern = 0;
    private int countValidSignals = 0;

    public TrendRSIBBBand(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = 20.0;
        this.riskReward = 3;
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

        System.out.println("=== DEBUG SUMMARY ===");
        System.out.println("Invalid trend:           " + countInvalidTrend);
        System.out.println("Did not touch BB:        " + countInvalidBB);
        System.out.println("Non-trading time:        " + countInvalidTime);
        System.out.println("RSI too high:            " + countInvalidRSI);
        System.out.println("Weak body / not green:   " + countInvalidBodyOrColor);
        System.out.println("No reversal pattern:     " + countInvalidPattern);
        System.out.println("Signals that passed all: " + countValidSignals);
    }

    private boolean strategyValid(int index) {
        Candle curr = candles.get(index);
        Candle prev = candles.get(index - 1);

        if (!TrendUtils.isBullishEnough(candles, index)) {
            countInvalidTrend++;
            return false;
        }

        BollingerBands bb = TrendUtils.getBollingerBands(candles, index, BOLLINGER_PERIOD);
        double touchThreshold = bb.lower * 1.03; // רק 2% מעל התחתון
        if (curr.getLow() > touchThreshold) {
            countInvalidBB++;
            return false;
        }

        if (!TimeUtils.isTradingHour(curr.getDate()) || TimeUtils.isSaturday(curr.getDate())) {
            countInvalidTime++;
            return false;
        }

        double avgRsiLast10 = TrendUtils.averageRSI(candles, index - 11, 11);
        double rsi = TrendUtils.calculateRSI(candles, index, 14);

        double rsiBig = TrendUtils.calculateRSI(candles, index, 21);

        if ( (rsi > avgRsiLast10 - 9) && (rsiBig > 42) ) {
            countInvalidRSI++;
            return false;
        }

        if (!CandleUtils.hasStrongBody(curr) || !CandleUtils.isGreen(curr)) {
            countInvalidBodyOrColor++;
            return false;
        }

        if (!(CandleUtils.isHammer(curr) || CandleUtils.isBullishEngulfing(prev, curr))) {
            countInvalidPattern++;
            return false;
        }

        countValidSignals++;
        return true;
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
        return new Signal(index, entryPrice, takeProfitPrice, stopLossPrice, positionSize);
    }
}
