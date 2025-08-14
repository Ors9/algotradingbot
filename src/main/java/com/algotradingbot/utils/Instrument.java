package com.algotradingbot.utils;

public enum Instrument {
    EURUSD(0.0001, QuoteMode.FIXED, 1.0),
    GBPUSD(0.0001, QuoteMode.FIXED, 1.0),
    USDJPY(0.01, QuoteMode.USD_IS_BASE, 0.0), // הערך המספרי לא בשימוש במצב הזה
    BTCUSD(1.0, QuoteMode.FIXED, 1.0),
    AUDUSD(0.0001, QuoteMode.FIXED, 1.0),
    USDCAD(0.0001, QuoteMode.USD_IS_BASE, 0.0),
    EURGBP(0.0001, QuoteMode.CROSS_TO_USD, 0.0);

    public enum QuoteMode {
        FIXED, USD_IS_BASE, CROSS_TO_USD
    }

    private final double pipSize;
    private final QuoteMode mode;
    private final double fixedQuoteToUsd; // רלוונטי רק ל-FIXED

    Instrument(double pipSize, QuoteMode mode, double fixedQuoteToUsd) {
        this.pipSize = pipSize;
        this.mode = mode;
        this.fixedQuoteToUsd = fixedQuoteToUsd;
    }

    public double getPipSize() {
        return pipSize;
    }

    /**
     * כמה USD זה שווה ליחידה אחת של שינוי במחיר (delta) עבור צמד נתון.
     * currentPrice = מחיר נוכחי של הצמד (לדוגמה 1.09650 ל-EURUSD, או 157.20
     * ל-USDJPY) crossRateIfNeeded = לדוגמה GBPUSD כאשר הצמד הוא EURGBP (כמה USD
     * לכל 1 GBP)
     */
    public double quoteToUSD(double currentPrice, Double crossRateIfNeeded) {
        switch (mode) {
            case FIXED:
                return fixedQuoteToUsd; // לדוגמה 1.0 עבור EURUSD
            case USD_IS_BASE:
                // USDJPY, USDCAD וכו' → השינוי נקוב במטבע ה-quote, ממירים ל-USD ע"י חלוקה במחיר
                return 1.0 / currentPrice;
            case CROSS_TO_USD:
                if (crossRateIfNeeded == null || crossRateIfNeeded <= 0.0) {
                    throw new IllegalArgumentException("Missing/invalid crossRateIfNeeded for " + this);
                }
                // לדוגמה: EURGBP * GBPUSD → שווי ב-USD
                return crossRateIfNeeded;
            default:
                throw new IllegalStateException("Unhandled mode " + mode);
        }
    }
}
