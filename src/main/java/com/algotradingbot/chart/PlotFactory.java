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
        TimeSeriesCollection dataset = new TimeSeriesCollection(DatasetFactory.createVolumeSeries(candles));

        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setShadowVisible(false);
        renderer.setMargin(0.2);
        renderer.setBarPainter(new org.jfree.chart.renderer.xy.StandardXYBarPainter());
        renderer.setSeriesPaint(0, new Color(0, 102, 204));

        // חישוב טווח מותאם
        double minVolume = Double.MAX_VALUE;
        double maxVolume = Double.MIN_VALUE;
        int startIndex = Math.max(0, candles.size() - 1000); // קח את 1000 האחרונים

        for (int i = startIndex; i < candles.size(); i++) {
            double vol = candles.get(i).getVolume();
            if (vol < minVolume) {
                minVolume = vol;
            }
            if (vol > maxVolume) {
                maxVolume = vol;
            }
        }

        // ציר Y עם טווח ידני
        NumberAxis axis = new NumberAxis("Volume");
        axis.setAutoRange(false);
        axis.setRange(minVolume * 0.9, maxVolume * 1.1); // מרווח קל למעלה ולמטה

        XYPlot plot = new XYPlot(dataset, null, axis, renderer);
        plot.setDomainPannable(true);
        plot.setRangePannable(false);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        return plot;
    }

    public static XYPlot createCandlePlot(DefaultHighLowDataset dataset, NumberAxis priceAxis) {
        CandlestickRenderer renderer = new CandlestickRenderer();
        renderer.setUpPaint(new Color(0, 153, 0));
        renderer.setDownPaint(new Color(204, 0, 0));
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setUseOutlinePaint(true);
        renderer.setDrawVolume(false);
        XYPlot plot = new XYPlot(dataset, null, priceAxis, renderer);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        return plot;
    }

    public static XYPlot createRSIPlot(ArrayList<Candle> candles) {
        return createRSIPlot(candles, 14);
    }

    public static XYPlot createRSIPlot(ArrayList<Candle> candles, int period) {
        TimeSeries rsiSeries = new TimeSeries("RSI");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = period; i < candles.size(); i++) {
            Double rsi = TrendUtils.calculateRSI(candles, i, period);
            if (rsi != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), formatter);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                rsiSeries.addOrUpdate(time, rsi);
            }
        }

        TimeSeriesCollection rsiDataset = new TimeSeriesCollection(rsiSeries);
        NumberAxis rsiAxis = new NumberAxis("RSI");
        rsiAxis.setRange(0, 100);

        XYLineAndShapeRenderer rsiRenderer = new XYLineAndShapeRenderer(true, false);
        rsiRenderer.setSeriesPaint(0, Color.ORANGE);

        XYPlot rsiPlot = new XYPlot(rsiDataset, null, rsiAxis, rsiRenderer);
        rsiPlot.setDomainPannable(true);
        rsiPlot.setRangePannable(false);

        ValueMarker marker70 = new ValueMarker(70);
        marker70.setPaint(Color.GRAY);
        marker70.setStroke(new BasicStroke(1f));

        ValueMarker marker30 = new ValueMarker(30);
        marker30.setPaint(Color.GRAY);
        marker30.setStroke(new BasicStroke(1f));

        rsiPlot.addRangeMarker(marker70);
        rsiPlot.addRangeMarker(marker30);

        return rsiPlot;
    }

    private static void addSMA(XYPlot plot, ArrayList<Candle> candles, int period, int datasetIndex, Color color) {
        TimeSeries series = DatasetFactory.createSMASeries(candles, period);
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        plot.setDataset(datasetIndex, dataset);
        XYLineAndShapeRenderer renderer = RendererFactory.createLineRenderer(color, 1.5f);
        plot.setRenderer(datasetIndex, renderer);
    }

    private static void addBollinger(XYPlot plot, ArrayList<Candle> candles, int period, int baseIndex) {
        TimeSeriesCollection[] bands = DatasetFactory.createBollingerDatasets(candles, period);

        plot.setDataset(baseIndex, bands[0]);
        plot.setRenderer(baseIndex, RendererFactory.createLineRenderer(Color.GRAY, 1f));

        plot.setDataset(baseIndex + 1, bands[1]);
        plot.setRenderer(baseIndex + 1, RendererFactory.createLineRenderer(Color.GRAY, 1f));

        plot.setDataset(baseIndex + 2, bands[2]);
        plot.setRenderer(baseIndex + 2, RendererFactory.createLineRenderer(Color.BLACK, 1.2f));
    }
}
