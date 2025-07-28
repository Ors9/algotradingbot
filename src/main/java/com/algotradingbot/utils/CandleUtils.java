package com.algotradingbot.utils;

import com.algotradingbot.core.Candle;

public class CandleUtils {

    public static boolean isGreen(Candle c) {
        return c.getClose() > c.getOpen();
    }

    public static boolean hasStrongBody(Candle c) {
        double range = c.getHigh() - c.getLow();
        if (range == 0) {
            return false;
        }
        return Math.abs(c.getClose() - c.getOpen()) / range >= 0.5;
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

    public static boolean isDoji(Candle c) {
        double body = Math.abs(c.getClose() - c.getOpen());
        double range = c.getHigh() - c.getLow();
        return body <= 0.1 * range;
    }
}
