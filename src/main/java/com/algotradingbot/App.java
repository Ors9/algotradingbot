package com.algotradingbot;

public class App {
    public static void main(String[] args) {
        BacktestEngine bte = new BacktestEngine();
        bte.loadCsv("data/sample.csv"); 
        bte.printRecords();
        System.out.println("=========Print Signals======");
        Strategy strategy = new Strategy(bte.getCandles());

        double signalsBuy = strategy.runBackTest();
        

    }
}