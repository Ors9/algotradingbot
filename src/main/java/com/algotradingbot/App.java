package com.algotradingbot;

public class App {

    public static void main(String[] args) {
        String json = "";
        try {
            json = getDataFromBinance.fetchKlines("BTCUSDT", "15m", 1000); // או כל טיקר אחר

        } catch (Exception e) {
            System.err.println("Failed to fetch data from Binance: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        BacktestEngine bte = new BacktestEngine();
        bte.parseCandles(json);
        bte.printRecords();


        System.out.println("=========Print Signals======");
        Strategy strategy = new Strategy(bte.getCandles());
        strategy.runBackTest();
        strategy.evaluteSignals();
        strategy.showResult();

    }



    
}
