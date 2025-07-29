package com.algotradingbot.core;

public class Performance {

    private int winCount;
    private int lossCount;
    private double winRate;
    private double totalProfit;
    private double maxDrawdown;

    // Constructor
    public Performance(int winCount, int lossCount, double totalProfit, double maxDrawdown) {
        this.winCount = winCount;
        this.lossCount = lossCount;
        this.totalProfit = totalProfit;
        this.maxDrawdown = maxDrawdown;
        int total = winCount + lossCount;
        this.winRate = total > 0 ? 100.0 * winCount / total : 0;
    }

    // Getters...
    public Performance add(Performance other) {
        int combinedWin = this.winCount + other.winCount;
        int combinedLoss = this.lossCount + other.lossCount;
        double combinedProfit = this.totalProfit + other.totalProfit;
        double worstDrawdown = Math.max(this.maxDrawdown, other.maxDrawdown); // conservative

        return new Performance(combinedWin, combinedLoss, combinedProfit, worstDrawdown);
    }

    public void print() {
        int total = winCount + lossCount;
        double winRate = total > 0 ? 100.0 * winCount / total : 0;

        System.out.printf("W:%-3d | L:%-3d | WinRate: %5.2f%% | Profit: $%8.2f | MaxDD: $%8.2f\n",
                winCount, lossCount, winRate, totalProfit, maxDrawdown);
    }

    @Override
    public String toString() {
        int total = winCount + lossCount;
        double winRate = total > 0 ? 100.0 * winCount / total : 0;

        return String.format("W:%-3d | L:%-3d | WinRate: %5.2f%% | Profit: $%8.2f | MaxDD: $%8.2f",
                winCount, lossCount, winRate, totalProfit, maxDrawdown);
    }
}
