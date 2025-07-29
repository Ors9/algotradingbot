package com.algotradingbot.chart;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;

import com.algotradingbot.core.Candle;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.TrendUtils;

public class DatasetFactory {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static DefaultHighLowDataset createCandleDataset(ArrayList<Candle> candles) {
        int count = candles.size();
        java.util.Date[] dates = new java.util.Date[count];
        double[] highs = new double[count];
        double[] lows = new double[count];
        double[] opens = new double[count];
        double[] closes = new double[count];
        double[] volumes = new double[count];

        for (int i = 0; i < count; i++) {
            Candle c = candles.get(i);
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), FORMATTER);
            dates[i] = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            highs[i] = c.getHigh();
            lows[i] = c.getLow();
            opens[i] = c.getOpen();
            closes[i] = c.getClose();
            volumes[i] = c.getVolume();
        }

        return new DefaultHighLowDataset("Candles", dates, highs, lows, opens, closes, volumes);
    }

    public static TimeSeriesCollection createVolumeDataset(ArrayList<Candle> candles) {
        TimeSeries series = new TimeSeries("Volume");
        for (Candle c : candles) {
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), FORMATTER);
            series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), c.getVolume());
        }
        return new TimeSeriesCollection(series);
    }

    public static TimeSeriesCollection createSMADataset(ArrayList<Candle> candles, int period) {
        TimeSeries series = new TimeSeries("SMA" + period);
        for (int i = period - 1; i < candles.size(); i++) {
            double sma;
            try {
                sma = TrendUtils.calculateSMA(candles, i, period);
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), FORMATTER);
                series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), sma);
            } catch (Exception ex) {
            }

        }
        return new TimeSeriesCollection(series);
    }

    public static TimeSeriesCollection createRSIDataset(ArrayList<Candle> candles, int period) {
        TimeSeries series = new TimeSeries("RSI");
        for (int i = period; i < candles.size(); i++) {
            Double rsi = TrendUtils.calculateRSI(candles, i, period);
            if (rsi != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), FORMATTER);
                series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), rsi);
            }
        }
        return new TimeSeriesCollection(series);
    }

    public static TimeSeriesCollection[] createBollingerDatasets(ArrayList<Candle> candles, int period) {
        TimeSeries upper = new TimeSeries("Upper Band");
        TimeSeries lower = new TimeSeries("Lower Band");
        TimeSeries middle = new TimeSeries("Middle Band");

        for (int i = period - 1; i < candles.size(); i++) {
            BollingerBands bb = TrendUtils.getBollingerBands(candles, i, period);
            if (bb != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), FORMATTER);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                upper.add(time, bb.upper);
                lower.add(time, bb.lower);
                middle.add(time, bb.sma);
            }
        }

        return new TimeSeriesCollection[]{
            new TimeSeriesCollection(upper),
            new TimeSeriesCollection(lower),
            new TimeSeriesCollection(middle)
        };
    }
}
