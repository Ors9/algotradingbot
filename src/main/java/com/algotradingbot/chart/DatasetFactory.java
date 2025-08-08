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

    public static DefaultHighLowDataset createDataset(ArrayList<Candle> candles) {
        return createCandleDataset(candles); // avoid redundancy
    }

    public static TimeSeriesCollection createBollingerSeries(ArrayList<Candle> candles, int period, String type) {
        TimeSeries series = new TimeSeries(type);
        for (int i = period - 1; i < candles.size(); i++) {
            BollingerBands bb = TrendUtils.getBollingerBands(candles, i, period);
            if (bb != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), FORMATTER);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                double value;
                switch (type) {
                    case "Upper Band":
                        value = bb.getUpper();
                        break;
                    case "Lower Band":
                        value = bb.getLower();
                        break;
                    case "Middle Band":
                        value = bb.getSma();
                        break;
                    default:
                        value = 0;
                }
                series.addOrUpdate(time, value);
            }
        }
        return new TimeSeriesCollection(series);
    }

    public static TimeSeriesCollection createSMADataset(ArrayList<Candle> candles, int period) {
        TimeSeries series = new TimeSeries("SMA" + period);
        for (int i = period - 1; i < candles.size(); i++) {
            try {
                double sma = TrendUtils.calculateSMA(candles, i, period);
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), FORMATTER);
                series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), sma);
            } catch (Exception ignored) {
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
                upper.add(time, bb.getUpper());
                lower.add(time, bb.getLower());
                middle.add(time, bb.getSma());
            }
        }

        return new TimeSeriesCollection[]{
            new TimeSeriesCollection(upper),
            new TimeSeriesCollection(lower),
            new TimeSeriesCollection(middle)
        };
    }

    public static TimeSeries createVolumeSeries(ArrayList<Candle> candles) {
        TimeSeries series = new TimeSeries("Volume");
        for (Candle c : candles) {
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), FORMATTER);
            series.addOrUpdate(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), c.getVolume());
        }
        return series;
    }

    public static TimeSeries createSMASeries(ArrayList<Candle> candles, int period) {
        TimeSeries smaSeries = new TimeSeries("SMA" + period);
        for (int i = period - 1; i < candles.size(); i++) {
            try {
                double smaValue = TrendUtils.calculateSMA(candles, i, period);
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), FORMATTER);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                smaSeries.addOrUpdate(time, smaValue);
            } catch (Exception e) {
                System.err.println("Error computing SMA at index " + i + ": " + e.getMessage());
            }
        }
        return smaSeries;

    }
}
