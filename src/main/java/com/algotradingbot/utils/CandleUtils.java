package com.algotradingbot.utils;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;

public class CandleUtils {

    public static boolean isGreen(Candle c) {
        return c.getClose() > c.getOpen();
    }


    public static boolean isInvertedHammer(Candle candle) {
        double body = Math.abs(candle.getClose() - candle.getOpen());
        double upperShadow = candle.getHigh() - Math.max(candle.getClose(), candle.getOpen());
        double lowerShadow = Math.min(candle.getClose(), candle.getOpen()) - candle.getLow();

        // גוף קטן, צל עליון ארוך, צל תחתון קצר מאוד
        return body > 0
                && upperShadow >= 2 * body
                && lowerShadow <= 0.1 * body;
    }

    // מגע נקי עם BB תחתון – סגירה מעל הקו, אבל הנמוך נוגע או חוצה אותו
    public static boolean isCleanLowerBBTouch(Candle c, BollingerBands bb) {
        if (bb == null) {
            return false;
        }
        return c.getLow() <= bb.getLower() && c.getClose() > bb.getLower();
    }

    public static boolean isGreenWithStrongLowerWick(Candle c, double minRatio) {
        double open = c.getOpen(), close = c.getClose(), high = c.getHigh(), low = c.getLow();
        double body = Math.abs(close - open);
        double range = high - low;
        if (range <= 0 || body == 0) {
            return false;
        }

        double lowerWick = Math.min(open, close) - low;
        // תנאים: נר ירוק + פתיל תחתון >= minRatio * גוף
        return close > open && lowerWick >= minRatio * body;
    }

    // גרסה נוחה עם ברירת־מחדל 1.5×
    public static boolean isGreenWithStrongLowerWick(Candle c) {
        return isGreenWithStrongLowerWick(c, 1.5);
    }

    public static boolean isTweezerBottom(Candle prev, Candle curr) {
        if (!Candle.isRed(prev) || !isGreen(curr)) {
            return false;
        }

        double low1 = prev.getLow();
        double low2 = curr.getLow();
        if (Math.abs(low1 - low2) > 0.008 * low1) { // שפל דומה ב־2.5%
            return false;
        }

        // גוף בינוני או חזק לנר הירוק
        if (!CandleUtils.hasStrongBody(curr)) {
            return false;
        }

        // סגירה של הנר הירוק גבוהה מהסגירה הקודמת
        if (curr.getClose() <= prev.getHigh() * 1.003) {
            return false;
        }

        return true;
    }

    public static boolean hasMediumOrStrongBody(Candle c) {
        double range = c.getHigh() - c.getLow();
        if (range == 0) {
            return false;
        }
        double bodyRatio = Math.abs(c.getClose() - c.getOpen()) / range;
        return bodyRatio >= 0.4; // בין 0.4 ל־1 נחשב בינוני או חזק
    }

    public static boolean isThreeWhiteSoldiers(Candle c1, Candle c2, Candle c3) {
        // נרות ירוקים
        if (!CandleUtils.isGreen(c1) || !CandleUtils.isGreen(c2) || !CandleUtils.isGreen(c3)) {
            return false;
        }

        // גוף לא חייב להיות חזק מאוד, מספיק בינוני ומעלה
        if (!CandleUtils.hasMediumOrStrongBody(c1)
                || !CandleUtils.hasMediumOrStrongBody(c2)
                || !CandleUtils.hasMediumOrStrongBody(c3)) {
            return false;
        }

        if (c2.getOpen() > c1.getClose() * 1.005 || c3.getOpen() > c2.getClose() * 1.005) {
            return false;
        }

        // נאפשר סגירה כמעט זהה (אפילו ירידה קטנה)
        if (c2.getClose() < c1.getClose() * 0.998 || c3.getClose() < c2.getClose() * 0.998) {
            return false;
        }

        return true;
    }

    public static boolean isBullishHarami(Candle prev, Candle curr) {
        if (!Candle.isRed(prev) || !isGreen(curr)) {
            return false;
        }

        return curr.getOpen() > prev.getLow()
                && curr.getClose() < prev.getHigh()
                && curr.getOpen() < prev.getClose()
                && curr.getClose() > prev.getOpen();
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

    public static boolean isEveningStar(Candle prev2, Candle prev1, Candle curr) {
        if (!isGreen(prev2) || !hasStrongBody(prev2)) {
            return false;
        }

        boolean smallBody = Math.abs(prev1.getClose() - prev1.getOpen())
                < 0.5 * Math.abs(prev2.getClose() - prev2.getOpen());

        boolean gapUp = prev1.getLow() > prev2.getHigh();
        boolean redCloseBelowHalf = Candle.isRed(curr)
                && curr.getClose() < (prev2.getOpen() + prev2.getClose()) / 2;

        return smallBody && gapUp && redCloseBelowHalf;
    }

    public static boolean isThreeBlackCrows(Candle c1, Candle c2, Candle c3) {
        if (!Candle.isRed(c1) || !hasStrongBody(c1)) {
            return false;
        }
        if (!Candle.isRed(c2) || !hasStrongBody(c2)) {
            return false;
        }
        if (!Candle.isRed(c3) || !hasStrongBody(c3)) {
            return false;
        }

        boolean opensWithinPrev = c2.getOpen() < c1.getOpen() && c2.getOpen() > c1.getClose()
                && c3.getOpen() < c2.getOpen() && c3.getOpen() > c2.getClose();

        boolean closesLower = c2.getClose() < c1.getClose() && c3.getClose() < c2.getClose();

        return opensWithinPrev && closesLower;
    }

}
