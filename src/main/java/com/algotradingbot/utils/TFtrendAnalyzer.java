package com.algotradingbot.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.engine.CandlesEngine;
import com.algotradingbot.engine.getDataFromBinance;

public class TFtrendAnalyzer {

    private final int SMA_DAYS = 200;

    private final ArrayList<Candle> candles1D;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TFtrendAnalyzer(LocalDateTime endDate, String symbol, String interval) {
        this.candles1D = new ArrayList<>();
        this.endDate = endDate;
        this.startDate = endDate.minusDays(SMA_DAYS);

        try {
            long start = toMillis(this.startDate);
            long end = toMillis(this.endDate);

            String json = getDataFromBinance.fetchKlinesRange(symbol, interval, start, end);
            CandlesEngine bte = new CandlesEngine();
            bte.parseCandles(json);
            candles1D.addAll(bte.getCandles());

        } catch (Exception e) {
            System.err.println("TFtrendAnalyzer fetch error: " + e.getMessage());
        }
    }

    private long toMillis(LocalDateTime dt) {
        return dt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    public ArrayList<Candle> getCandles1D() {
        return candles1D;
    }

    public double smaCalc(int period) {
        if (candles1D.size() < period) {
            return -1;
        }

        double sum = 0;
        for (int i = candles1D.size() - period; i < candles1D.size(); i++) {
            sum += candles1D.get(i).getClose();
        }
        return sum / period;
    }
}
