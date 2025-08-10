package com.algotradingbot.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class RollingWindow {

    private final int capacity;
    private final ArrayDeque<Candle> dq = new ArrayDeque<>();

    public RollingWindow(int capacity) {
        this.capacity = capacity;
    }

    public void addClosedCandle(Candle c) {
        dq.addLast(c);
        if (dq.size() > capacity) {
            dq.removeFirst();
        }
    }

    public boolean isReady() {
        return dq.size() == capacity;
    }

    public List<Candle> view() {
        return new ArrayList<>(dq);
    }

    public Candle last() {
        return dq.peekLast();
    }

    public long lastCloseTime() {
        return dq.isEmpty() ? -1 : dq.peekLast().getDateMillis();
    }
}
