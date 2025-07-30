package com.algotradingbot.utils;

import com.algotradingbot.core.Candle;

public class CandleUtils {

    public static boolean isGreen(Candle c) {
        return c.getClose() > c.getOpen();
    }

    public static boolean isAlmostGreen(Candle c) {
        double diff = c.getClose() - c.getOpen();
        double range = c.getHigh() - c.getLow();
        return diff >= 0 || (diff > -0.1 * range); // לא ירידה דרמטית
    }

    public static boolean hasStrongBody(Candle c) {
        double range = c.getHigh() - c.getLow();
        if (range == 0) {
            return false;
        }
        return Math.abs(c.getClose() - c.getOpen()) / range >= 0.55;
    }

    public static boolean isShootingStar(Candle c) {
        double open = c.getOpen();
        double close = c.getClose();
        double high = c.getHigh();
        double low = c.getLow();

        double body = Math.abs(close - open);
        double upperShadow = high - Math.max(open, close);
        double lowerShadow = Math.min(open, close) - low;

        // גוף קטן יחסית
        boolean smallBody = body <= (high - low) * 0.3;

        // צל עליון ארוך פי 2 לפחות מהגוף
        boolean longUpperShadow = upperShadow >= body * 2;

        // כמעט בלי צל תחתון
        boolean smallLowerShadow = lowerShadow <= body * 0.2;

        return smallBody && longUpperShadow && smallLowerShadow;
    }

    public static boolean isBearishEngulfing(Candle prev, Candle curr) {
        return Candle.isRed(curr) && isGreen(prev)
                && curr.getOpen() > prev.getClose()
                && curr.getClose() < prev.getOpen();
    }

    public static boolean isBullishEngulfing(Candle prev, Candle curr) {
        return (prev.getClose() < prev.getOpen())
                && // נר קודם אדום
                (curr.getClose() > curr.getOpen())
                && // נר נוכחי ירוק
                (curr.getOpen() < prev.getClose())
                && (curr.getClose() > prev.getOpen());
    }

    public static boolean isInsideBar(Candle prev, Candle curr) {
        double margin = 0.002 * prev.getHigh();
        return curr.getHigh() <= prev.getHigh() + margin
                && curr.getLow() >= prev.getLow() - margin;
    }

    public static boolean isHammer(Candle c) {
        double body = Math.abs(c.getClose() - c.getOpen());
        double lowerWick = Math.min(c.getOpen(), c.getClose()) - c.getLow();
        double upperWick = c.getHigh() - Math.max(c.getOpen(), c.getClose());

        return lowerWick > 2 * body && upperWick < body;
    }

    // Piercing Line: נר ירוק שחודר לפחות 50% מגוף אדום קודם
    public static boolean isPiercingLine(Candle prev, Candle curr) {
        if (!Candle.isRed(prev) || !isGreen(curr)) {
            return false;
        }

        double prevBodyMid = (prev.getOpen() + prev.getClose()) / 2;
        return curr.getOpen() < prev.getLow() && curr.getClose() > prevBodyMid;
    }

    // Morning Star: שלישייה – ירידה חזקה, נר קטן, ואז עלייה חזקה
    public static boolean isMorningStar(Candle c1, Candle c2, Candle c3) {
        return Candle.isRed(c1)
                && Candle.isSmallBody(c2)
                && isGreen(c3)
                && c3.getClose() > ((c1.getOpen() + c1.getClose()) / 2);
    }

    public static boolean isDoji(Candle c) {
        double body = Math.abs(c.getClose() - c.getOpen());
        double range = c.getHigh() - c.getLow();
        return body <= 0.1 * range;
    }
}
