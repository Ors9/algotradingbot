package com.algotradingbot.engine;

import com.algotradingbot.core.BrokerBinanceManager;
import com.algotradingbot.strategies.BBbandWithComma;

public final class DemoBrokerBinanceManager extends BrokerBinanceManager {

    private final String symbol;
    private final String interval; // למשל "4h"
    private final BBbandWithComma strategyManager;
    private long lastCloseTime = -1L; // millis של הנר האחרון בחלון

    public DemoBrokerBinanceManager(String symbol, String interval, BBbandWithComma strategyManager) {
        super(); // יוצר RollingWindow(200) במחלקת האב
        this.symbol = symbol;
        this.interval = interval;
        this.strategyManager = strategyManager;
    }

    @Override
    public String placeMarketOrder(String symbol, boolean isLong, double qty) {
        /* ... */ return "OK";
    }

    @Override
    public String placeStopLoss(String symbol, boolean isLong, double qty, double stopPrice) {
        /* ... */ return "OK";
    }

    @Override
    public String placeTakeProfit(String symbol, boolean isLong, double qty, double tpPrice) {
        /* ... */ return "OK";
    }

    @Override
    public void cancelAll(String symbol) {
        /* ... */ }
}
