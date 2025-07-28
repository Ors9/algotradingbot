package com.algotradingbot.core;

public class Candle {

    public String date;
    public double open, high, low, close;

    public Candle(String date, double open, double high, double low, double close) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    @Override
    public String toString() {
        return "Candle{"
                + "date='" + date + '\''
                + ", open=" + open
                + ", high=" + high
                + ", low=" + low
                + ", close=" + close
                + '}';
    }

    public String getDate() {
        return date;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

}
