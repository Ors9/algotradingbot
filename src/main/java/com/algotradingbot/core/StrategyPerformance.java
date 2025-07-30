package com.algotradingbot.core;

public class StrategyPerformance {

    private final Performance longPerformance;
    private final Performance shortPerformance;

    public StrategyPerformance(Performance longPerformance, Performance shortPerformance) {
        this.longPerformance = longPerformance;
        this.shortPerformance = shortPerformance;
    }

    public StrategyPerformance add(StrategyPerformance other) {
        Performance combinedLong = this.longPerformance.add(other.longPerformance);
        Performance combinedShort = this.shortPerformance.add(other.shortPerformance);
        return new StrategyPerformance(combinedLong, combinedShort);
    }

    public Performance getLongPerformance() {
        return longPerformance;
    }

    public Performance getShortPerformance() {
        return shortPerformance;
    }

    public Performance getCombinedPerformance() {
        return longPerformance.add(shortPerformance);
    }

    public void print() {
        System.out.println("=== Long Performance ===");
        longPerformance.print();

        System.out.println("=== Short Performance ===");
        shortPerformance.print();

        System.out.println("=== Combined Performance ===");
        getCombinedPerformance().print();
    }

    @Override
    public String toString() {
        return "=== Long Performance ===\n" + longPerformance.toString()
                + "\n=== Short Performance ===\n" + shortPerformance.toString()
                + "\n=== Combined Performance ===\n" + getCombinedPerformance().toString();
    }
}
