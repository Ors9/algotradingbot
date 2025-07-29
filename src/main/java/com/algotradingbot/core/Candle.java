package com.algotradingbot.core;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Candle {

    public String date;
    private double open, high, low, close, volume;

    public Candle(String date, double open, double high, double low, double close, double volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "Candle{"
                + "date='" + date + '\''
                + ", open=" + open
                + ", high=" + high
                + ", low=" + low
                + ", close=" + close
                + ", volume=" + volume
                + '}';
    }

    public String getDate() {
        return date;
    }

    public long getDateMillis() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime ldt = LocalDateTime.parse(this.date, formatter);
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public double getBodyHeight() {
        return Math.abs(close - open);
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getVolume() {
        return volume;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

}
