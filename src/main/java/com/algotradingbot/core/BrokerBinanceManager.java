package com.algotradingbot.core;

public abstract class BrokerBinanceManager {
    protected  RollingWindow window;
    private final int CAPACITY = 200;
    protected BrokerBinanceManager() {
        this.window = new RollingWindow(CAPACITY);
    }

    public abstract String placeMarketOrder(String symbol, boolean isLong, double qty);

    public abstract String placeStopLoss(String symbol, boolean isLong, double qty, double stopPrice);

    public abstract String placeTakeProfit(String symbol, boolean isLong, double qty, double tpPrice);

    public abstract void cancelAll(String symbol);

    // אופציונלי: גישה ל־window
    public RollingWindow getWindow() {
        return window;
    }
}
