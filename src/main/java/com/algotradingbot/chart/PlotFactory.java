package com.algotradingbot.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;

import com.algotradingbot.core.Candle;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.TrendUtils;

public class PlotFactory {

    public static XYPlot createCandlePlot(ArrayList<Candle> candles) {
        DefaultHighLowDataset dataset = DatasetFactory.createCandleDataset(candles);
        CandlestickRenderer renderer = RendererFactory.createCandleRenderer();
        NumberAxis priceAxis = new NumberAxis("Price");
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setLowerMargin(0.05);
        priceAxis.setUpperMargin(0.05);

        XYPlot plot = new XYPlot(dataset, null, priceAxis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);  // ✅ Y

        // Add SMA
        addSMA(plot, candles, 20, 1, Color.BLUE);
        addSMA(plot, candles, 50, 2, Color.MAGENTA);

        // Add Bollinger Bands
        addBollinger(plot, candles, 20, 3);

        return plot;
    }

    public static XYPlot createVolumePlot(ArrayList<Candle> candles) {
        TimeSeries series = new TimeSeries("");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Candle c : candles) {
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), formatter);
            series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), c.getVolume());
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        XYBarRenderer renderer = RendererFactory.createVolumeRenderer();
        NumberAxis axis = new NumberAxis("Volume");
        XYPlot plot = new XYPlot(dataset, null, axis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainPannable(true);
        return plot;
    }

    public static XYPlot createRSIPlot(ArrayList<Candle> candles) {
        TimeSeries series = new TimeSeries("RSI");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (int i = 14; i < candles.size(); i++) {
            Double rsi = TrendUtils.calculateRSI(candles, i, 14);
            if (rsi != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), formatter);
                series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), rsi);
            }
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        NumberAxis axis = new NumberAxis("RSI");
        axis.setRange(0, 100);
        XYLineAndShapeRenderer renderer = RendererFactory.createLineRenderer(Color.ORANGE, 1.2f);
        XYPlot plot = new XYPlot(dataset, null, axis, renderer);

        ValueMarker marker70 = new ValueMarker(70, Color.GRAY, new BasicStroke(1f));
        ValueMarker marker30 = new ValueMarker(30, Color.GRAY, new BasicStroke(1f));
        plot.addRangeMarker(marker70);
        plot.addRangeMarker(marker30);
        return plot;
    }

    private static void addSMA(XYPlot plot, ArrayList<Candle> candles, int period, int datasetIndex, Color color) {
        TimeSeries series = new TimeSeries("SMA" + period);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (int i = period - 1; i < candles.size(); i++) {
            double sma;
            try {
                sma = TrendUtils.calculateSMA(candles, i, period);
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), formatter);
                series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), sma);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        plot.setDataset(datasetIndex, dataset);
        XYLineAndShapeRenderer renderer = RendererFactory.createLineRenderer(color, 1.5f);
        plot.setRenderer(datasetIndex, renderer);
    }

    private static void addBollinger(XYPlot plot, ArrayList<Candle> candles, int period, int baseIndex) {
        TimeSeries upper = new TimeSeries("Upper Band");
        TimeSeries lower = new TimeSeries("Lower Band");
        TimeSeries middle = new TimeSeries("Middle Band");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (int i = period - 1; i < candles.size(); i++) {
            BollingerBands bb = TrendUtils.getBollingerBands(candles, i, period);
            if (bb != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), formatter);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                upper.add(time, bb.upper);
                lower.add(time, bb.lower);
                middle.add(time, bb.sma);
            }
        }
        plot.setDataset(baseIndex, new TimeSeriesCollection(upper));
        plot.setRenderer(baseIndex, RendererFactory.createLineRenderer(Color.GRAY, 1f));
        plot.setDataset(baseIndex + 1, new TimeSeriesCollection(lower));
        plot.setRenderer(baseIndex + 1, RendererFactory.createLineRenderer(Color.GRAY, 1f));
        plot.setDataset(baseIndex + 2, new TimeSeriesCollection(middle));
        plot.setRenderer(baseIndex + 2, RendererFactory.createLineRenderer(Color.BLACK, 1.2f));
    }
}
