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
        this.riskReward = 2;
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

        System.out.println("=== סיכום DEBUG ===");
        System.out.println("📉 מגמה לא תקינה:         " + countInvalidTrend);
        System.out.println("📉 לא נגע ב-BB:            " + countInvalidBB);
        System.out.println("⏰ זמן לא מסחרי:          " + countInvalidTime);
        System.out.println("📊 RSI גבוה מדי:          " + countInvalidRSI);
        System.out.println("🕯️ גוף חלש / נר לא ירוק: " + countInvalidBodyOrColor);
        System.out.println("📉 בלי תבנית היפוך:       " + countInvalidPattern);
        System.out.println("✅ עסקאות שעברו הכל:       " + countValidSignals);
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

        double avgRsiLast10 = TrendUtils.averageRSI(candles, index - 10, 10);
        double rsi = TrendUtils.calculateRSI(candles, index, 14);
        if (rsi > avgRsiLast10 - 10) {
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
