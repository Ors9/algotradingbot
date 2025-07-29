package com.algotradingbot.chart;

import java.awt.BasicStroke; // Make sure this is imported
import java.awt.BorderLayout; // Optional for styling
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Performance;
import com.algotradingbot.core.Signal;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.TrendUtils;

public class CandleChart extends JFrame {

    public CandleChart(String title, ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        super(title);
        setLayout(new BorderLayout());

        DefaultHighLowDataset dataset = createDataset(candles);
        JFreeChart chart = createCombinedChart(dataset, candles, perf); // <- שינוי כאן
        CombinedDomainXYPlot combinedPlot = (CombinedDomainXYPlot) chart.getPlot();
        XYPlot candlePlot = (XYPlot) combinedPlot.getSubplots().get(0); // קבל את גרף הנרות
        candlePlot.setBackgroundPaint(Color.WHITE);
        candlePlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        candlePlot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        addSignalAnnotations(candlePlot, candles, signals); // תעביר לו את זה

        ChartPanel chartPanel = createInteractivePanel(chart);
        add(chartPanel, BorderLayout.CENTER);

    }

    private JFreeChart createCombinedChart(DefaultHighLowDataset candleDataset, ArrayList<Candle> candles, Performance perf) {
        // --- גרף נרות ---
        CandlestickRenderer candleRenderer = new CandlestickRenderer();

        candleRenderer.setSeriesPaint(0, Color.BLACK); // צבע של הגבולות

        // עיצוב נרות
        candleRenderer.setUpPaint(new Color(0, 153, 0));   // נר ירוק מלא (עלייה)
        candleRenderer.setDownPaint(new Color(204, 0, 0)); // נר אדום מלא (ירידה)

        candleRenderer.setUseOutlinePaint(true); // הפעלת ציור קווים חיצוניים
        candleRenderer.setDrawVolume(false); // ✅ מונע ציור עמודות ווליום בתוך גרף הנרות
        NumberAxis priceAxis = new NumberAxis("Price");

        XYPlot candlePlot = new XYPlot(candleDataset, null, priceAxis, candleRenderer);

        candlePlot.setDomainPannable(true); // ✅ פאן בציר הזמן
        candlePlot.setRangePannable(true);

        priceAxis.setAutoRange(true);
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setAutoRangeStickyZero(false); // ✅ חשוב – שלא ינסה "להדביק" את 0
        priceAxis.setLowerMargin(0.05); // קצת מרווח מתחת לנרות
        priceAxis.setUpperMargin(0.05); // קצת מרווח מעל לנרות
        // --- גרף Volume ---
        TimeSeries series = new TimeSeries(""); // <-- אין כותרת מיותרת
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Candle c : candles) {
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), formatter);
            series.addOrUpdate(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), c.getVolume());
        }
        TimeSeriesCollection volumeDataset = new TimeSeriesCollection(series);
        XYBarRenderer volumeRenderer = new XYBarRenderer();
        volumeRenderer.setShadowVisible(false);                   // ביטול הצל
        volumeRenderer.setMargin(0.1);                            // קצת רווח בין עמודות
        NumberAxis volumeAxis = new NumberAxis("Volume");
        volumeAxis.setAutoRange(true);
        volumeAxis.setAutoRangeIncludesZero(true);
        volumeAxis.setLowerMargin(0.05);
        volumeAxis.setUpperMargin(0.05);
        XYPlot volumePlot = new XYPlot(volumeDataset, null, volumeAxis, volumeRenderer);
        volumePlot.setDomainPannable(true);  // ✅ כאן כן מותר
        volumePlot.setRangePannable(false);  // ❌ לא נחוץ ב־Y

        // --- שילוב עם ציר זמן משותף ---
        DateAxis timeAxis = new DateAxis("Time");
        timeAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
        combinedPlot.add(candlePlot, 5); // משקל גדול יותר לנרות
        combinedPlot.add(volumePlot, 1); // משקל קטן ל-volume
        combinedPlot.setDomainPannable(true);
        // --- עיצוב ---
        combinedPlot.setBackgroundPaint(Color.WHITE);
        combinedPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        combinedPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // --- יצירת גרף ---
        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, false);

        // --- תוספת ביצועים ---
        TextTitle perfTitle = new TextTitle(perf.toString());
        perfTitle.setFont(new Font("Arial", Font.PLAIN, 12));
        perfTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        chart.addSubtitle(perfTitle);

        priceAxis.setAutoRange(true);
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        priceAxis.setLabelFont(new Font("Arial", Font.BOLD, 13));
        priceAxis.setTickLabelInsets(new org.jfree.chart.ui.RectangleInsets(2, 5, 2, 5)); // ריווח פנימי
        priceAxis.setLabelInsets(new org.jfree.chart.ui.RectangleInsets(5, 5, 5, 5));     // מרווח כותרת

        int smaDatasetIndex = 1;
        addSMAtoCandlePlot(candlePlot, candles, 20, smaDatasetIndex++, Color.BLUE);
        addSMAtoCandlePlot(candlePlot, candles, 50, smaDatasetIndex++, Color.GREEN);
        addSMAtoCandlePlot(candlePlot, candles, 200, smaDatasetIndex++, Color.RED);

        int bbDatasetStartIndex = smaDatasetIndex; // start after SMAs
        addBollingerBands(candlePlot, candles, 20, bbDatasetStartIndex); // indices 4, 5, 6

        XYPlot rsiPlot = createRSIPlot(candles, 14);
        combinedPlot.add(rsiPlot, 2); // Give RSI subplot smaller weight

        return chart;
    }

    private void addSMAtoCandlePlot(XYPlot candlePlot, ArrayList<Candle> candles, int period, int datasetIndex, Color color) {
        TimeSeries smaSeries = createSMASeries(candles, period);
        TimeSeriesCollection smaDataset = new TimeSeriesCollection();
        smaDataset.addSeries(smaSeries);

        candlePlot.setDataset(datasetIndex, smaDataset);

        org.jfree.chart.renderer.xy.XYLineAndShapeRenderer smaRenderer = new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true, false);
        smaRenderer.setSeriesPaint(0, color);
        smaRenderer.setSeriesStroke(0, new java.awt.BasicStroke(1.5f));

        candlePlot.setRenderer(datasetIndex, smaRenderer);
    }

    private void addSignalAnnotations(XYPlot plot, ArrayList<Candle> candles, ArrayList<Signal> signals) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Signal signal : signals) {
            if (!signal.isEvaluated()) {
                continue;
            }

            int index = signal.getIndex(); // ודא שיש שדה index או תחליף מתאים
            if (index < 0 || index >= candles.size()) {
                continue;
            }

            Candle candle = candles.get(index);
            LocalDateTime ldt = LocalDateTime.parse(candle.getDate(), formatter);
            double x = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime();
            double y = signal.getEntryPrice();

            String marker = signal.isWinSignal() ? "↑" : "↓";
            java.awt.Color color = signal.isWinSignal() ? java.awt.Color.GREEN : java.awt.Color.RED;

            XYTextAnnotation annotation = new XYTextAnnotation(marker, x, y);
            annotation.setFont(new Font("Arial", Font.BOLD, 14));
            annotation.setPaint(color);
            annotation.setTextAnchor(TextAnchor.BOTTOM_CENTER);

            plot.addAnnotation(annotation);
        }
    }

    private ChartPanel createInteractivePanel(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        panel.setMouseWheelEnabled(true);   // ✅ Zoom בגלגלת
        panel.setMouseZoomable(true);       // ✅ חובה כדי לאפשר גלגלת

        panel.setDomainZoomable(true);      // ✅ מאפשר Zoom בציר זמן
        panel.setRangeZoomable(true);

        panel.setHorizontalAxisTrace(false);
        panel.setVerticalAxisTrace(false);
        panel.setPreferredSize(new Dimension(1600, 800));

        return panel;
    }

    private DefaultHighLowDataset createDataset(ArrayList<Candle> candles) {
        int itemCount = candles.size();
        java.util.Date[] dates = new java.util.Date[itemCount];
        double[] highs = new double[itemCount];
        double[] lows = new double[itemCount];
        double[] opens = new double[itemCount];
        double[] closes = new double[itemCount];
        double[] volumes = new double[itemCount];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (int i = 0; i < itemCount; i++) {

            Candle c = candles.get(i);

            // Parse the string date to LocalDateTime
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), formatter);

            // Convert LocalDateTime to java.util.Date for the chart
            dates[i] = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());

            highs[i] = c.getHigh();
            lows[i] = c.getLow();
            opens[i] = c.getOpen();
            closes[i] = c.getClose();
            volumes[i] = c.getVolume();
        }
        return new DefaultHighLowDataset("Candles", dates, highs, lows, opens, closes, volumes);
    }

    public static void showChart(ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        SwingUtilities.invokeLater(() -> {
            CandleChart chart = new CandleChart("Candlestick Chart", candles, signals, perf);
            chart.setSize(1000, 700);
            chart.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // ✅ רק סוגר את החלון הזה
            chart.setVisible(true);
        });
    }

    private TimeSeries createSMASeries(ArrayList<Candle> candles, int period) {
        TimeSeries smaSeries = new TimeSeries("SMA" + period);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = period - 1; i < candles.size(); i++) {
            try {
                double smaValue = TrendUtils.calculateSMA(candles, i, period);
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), formatter);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
                smaSeries.addOrUpdate(time, smaValue);
            } catch (Exception e) {
                System.err.println("Error computing SMA at index " + i + ": " + e.getMessage());
            }

        }

        return smaSeries;
    }

    private void addBollingerBands(XYPlot candlePlot, ArrayList<Candle> candles, int period, int datasetIndexBase) {
        TimeSeries upperSeries = new TimeSeries("Upper Band");
        TimeSeries lowerSeries = new TimeSeries("Lower Band");
        TimeSeries middleSeries = new TimeSeries("Middle Band");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = period - 1; i < candles.size(); i++) {
            BollingerBands bb = TrendUtils.getBollingerBands(candles, i, period);
            if (bb != null) {
                LocalDateTime ldt = LocalDateTime.parse(candles.get(i).getDate(), formatter);
                Minute time = new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));

                upperSeries.addOrUpdate(time, bb.upper);
                lowerSeries.addOrUpdate(time, bb.lower);
                middleSeries.addOrUpdate(time, bb.sma);
            }
        }

        TimeSeriesCollection upperDataset = new TimeSeriesCollection(upperSeries);
        TimeSeriesCollection lowerDataset = new TimeSeriesCollection(lowerSeries);
        TimeSeriesCollection middleDataset = new TimeSeriesCollection(middleSeries);

        // Add to plot
        candlePlot.setDataset(datasetIndexBase, upperDataset);
        candlePlot.setDataset(datasetIndexBase + 1, lowerDataset);
        candlePlot.setDataset(datasetIndexBase + 2, middleDataset);

        // Renderer for all bands (reuse same styling with different colors)
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setSeriesStroke(0, new BasicStroke(1f));
        candlePlot.setRenderer(datasetIndexBase, renderer);

        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesPaint(0, Color.BLACK);
        renderer2.setSeriesStroke(0, new BasicStroke(1f));
        candlePlot.setRenderer(datasetIndexBase + 1, renderer2);

        XYLineAndShapeRenderer renderer3 = new XYLineAndShapeRenderer(true, false);
        renderer3.setSeriesPaint(0, Color.BLUE); // SMA (middle band)
        renderer3.setSeriesStroke(0, new BasicStroke(1.2f));
        candlePlot.setRenderer(datasetIndexBase + 2, renderer3);
    }

    private XYPlot createRSIPlot(ArrayList<Candle> candles, int period) {
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
}
