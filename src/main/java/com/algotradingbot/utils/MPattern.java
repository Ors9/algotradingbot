package com.algotradingbot.utils;

import java.util.ArrayList;
import com.algotradingbot.core.Candle;

public class MPattern {

    private final ArrayList<Candle> candles;
    private final int currIndex;

    private Candle firstLegHigh; // H1 שנבחר

    // --- פרמטרים לוגיים ---
    private static final int RSI_PERIOD     = 14;
    private static final int LOOKBACK       = 50;  // חלון חיפוש לשיא הראשון
    private static final int MIN_GAP_BARS   = 8;   // ריווח מינימלי בין H1 ל-H2
    private static final int PRICE_RADIUS   = 7;  // "הכי גבוה ב-10 שמאלה/ימינה"
    private static final int RSI_RADIUS     = 5;   // RSI מקסימלי סביב בר אחד לשני הצדדים
    private static final double OB_LEVEL    = 70.0;
    private static final double DIVERGENCE_DELTA = 5.0; // נק' RSI
    private static final double BREAK_ATR_BUFFER_MULT = 0.10; // 0.1*ATR

    public MPattern(ArrayList<Candle> candles, int currIndex) {
        this.candles   = candles;
        this.currIndex = currIndex;
        this.firstLegHigh = null;
    }

    public boolean analyzeMPattern() {
        if (candles == null || candles.isEmpty()) return false;

        int bbPeriod = BollingerBands.BBPeriod.BB_20.getPeriod();
        if (currIndex <= Math.max(bbPeriod, RSI_PERIOD) || currIndex >= candles.size()) return false;

        Candle curr = candles.get(currIndex);

        // (0) H2 חייב לגעת Upper BB
        if (!TrendUtils.isTouchingUpperBB(candles, currIndex, bbPeriod)) return false;

        // חלון חיפוש H1 עם מרווח מינימלי
        int start = Math.max(0, currIndex - LOOKBACK);
        int end   = Math.max(start, currIndex - MIN_GAP_BARS);

        // נבחר H1 "הכי טוב" דטרמיניסטית
        int    bestIdx  = -1;
        double bestRsi1 = Double.NEGATIVE_INFINITY;
        double bestHigh = Double.NEGATIVE_INFINITY;

        for (int i = start; i <= end; i++) {
            // דרוש מקסימום מקומי במחיר בסביבה (PRICE_RADIUS)
            if (!isLocalMaxPrice(i, PRICE_RADIUS)) continue;

            // RSI סביב השיא (max(i-1..i+1))
            Double rsi1 = rsiMaxAround(i, RSI_RADIUS);
            if (rsi1 == null || rsi1 < OB_LEVEL) continue;

            double hi = candles.get(i).getHigh();

            // Pullback: לפחות סגירה אחת מתחת ל-Middle Band בין H1 ל-H2
            if (!hasMiddleBandPullbackBetween(i + 1, currIndex - 1, bbPeriod)) continue;

            // דירוג המועמד
            boolean better =
                (rsi1 > bestRsi1) ||
                (rsi1 == bestRsi1 && hi > bestHigh) ||
                (rsi1 == bestRsi1 && hi == bestHigh && i > bestIdx);

            if (better) {
                bestIdx  = i;
                bestRsi1 = rsi1;
                bestHigh = hi;
            }
        }

        if (bestIdx == -1) return false; // לא נמצא H1 מתאים

        firstLegHigh = candles.get(bestIdx);

        // (3) שבירת שיא + דיברג'נס
        double h1  = firstLegHigh.getHigh();
        double h2  = curr.getHigh(); // שבירה לפי High, לא Close

        boolean brokeHigh = (h2 > h1);

        Double rsi2Obj = TrendUtils.calculateRSI(candles, currIndex, RSI_PERIOD);
        if (rsi2Obj == null) return false;
        double rsi2 = rsi2Obj;

        boolean divergence = (rsi2 < bestRsi1 - DIVERGENCE_DELTA);

        if (brokeHigh && divergence) {
            // הדפסות (אם תרצה לוג)
            System.out.println("First Push " + firstLegHigh + " RSI1=" + bestRsi1);
             System.out.println("Final candle: " + curr + " RSI2=" + rsi2);
            return true;
        }

        return false;
    }

    // ---- עזרי לוגיקה ----

    private boolean isLocalMaxPrice(int idx, int radius) {
        int n = candles.size();
        if (idx - radius < 0 || idx + radius >= n) return false;
        double h = candles.get(idx).getHigh();
        for (int k = 1; k <= radius; k++) {
            if (candles.get(idx - k).getHigh() >= h) return false;
            if (candles.get(idx + k).getHigh() >= h) return false;
        }
        return true;
    }

    private Double rsiMaxAround(int idx, int radius) {
        Double best = null;
        for (int k = idx - radius; k <= idx + radius; k++) {
            if (k < 0 || k >= candles.size()) continue;
            Double r = TrendUtils.calculateRSI(candles, k, RSI_PERIOD);
            if (r == null) continue;
            best = (best == null) ? r : Math.max(best, r);
        }
        return best;
    }

    private boolean hasMiddleBandPullbackBetween(int from, int to, int bbPeriod) {
        if (from > to) return false;
        from = Math.max(from, 0);
        to   = Math.min(to, candles.size() - 1);
        for (int j = from; j <= to; j++) {
            BollingerBands bb = TrendUtils.getBollingerBands(candles, j, bbPeriod);
            if (bb == null) continue;
            if (candles.get(j).getClose() < bb.getSma()) {
                return true; // נמצא Pullback (סגירה מתחת ל-Middle)
            }
        }
        return false;
    }

    // Getter אם תרצה להשתמש ב-H1 מחוץ למחלקה
    public Candle getFirstLegHigh() { return firstLegHigh; }
}
