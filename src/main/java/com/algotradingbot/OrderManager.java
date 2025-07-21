package com.algotradingbot;

/**
 * Responsible for placing, modifying, and canceling orders via Binance API.
 * Uses BinanceApiClient for sending trade orders.
 */
public class OrderManager {

    private static final double RISK_PER_TRADE_USD = 20.0;

    public Position createPositionFromSignal(Signal signal) {
        double stopSize = Math.abs(signal.getEntryPrice() - signal.getStopPrice());

        if (stopSize == 0) {
            System.err.println("Invalid stop size (0) for signal: " + signal);
            return null;
        }

        double positionSize = RISK_PER_TRADE_USD / stopSize;

        return new Position(signal, positionSize);
    }
}

