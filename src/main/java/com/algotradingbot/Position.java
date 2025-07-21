package com.algotradingbot;

public class Position {
    private final Signal signal;
    private final double size;

    public Position(Signal signal, double size) {
        this.signal = signal;
        this.size = size;
    }

    public double getPnl() {
        return signal.isWinSignal()
                ? (signal.getTpPrice() - signal.getEntryPrice()) * size
                : (signal.getStopPrice() - signal.getEntryPrice()) * size;
    }

    @Override
    public String toString() {
        return String.format("Position{entry=%.2f, size=%.2f, result=%s, PnL=%.2f}",
                signal.getEntryPrice(), size,
                signal.isWinSignal() ? "WIN" : "LOSS",
                getPnl());
    }
}

