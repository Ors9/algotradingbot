package com.algotradingbot.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Performance;
import com.algotradingbot.core.Signal;

public class AnnotationUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font ARROW_FONT = new Font("Arial", Font.BOLD, 25);
    private static final Font PERFORMANCE_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final float DEFAULT_SMA_STROKE = 1.5f;

    /**
     * Adds signal annotations (arrows) to the candle plot.
     */
    public static void addSignalAnnotations(XYPlot plot, ArrayList<Candle> candles, ArrayList<Signal> signals) {
        for (Signal signal : signals) {
            if (!signal.isEvaluated() || signal.getIndex() < 0 || signal.getIndex() >= candles.size()) {
                continue;
            }

            Candle candle = candles.get(signal.getIndex());
            double x = getTimeAsMilliseconds(candle.getDate());
            double y = getAnnotationY(candle);

            XYTextAnnotation annotation = new XYTextAnnotation("↓", x, y);
            annotation.setFont(ARROW_FONT);
            annotation.setPaint(signal.isWinSignal() ? Color.GREEN : Color.RED);
            annotation.setTextAnchor(TextAnchor.CENTER);

            plot.addAnnotation(annotation);
        }
    }

    public static void addTradeAnnotations(XYPlot plot, ArrayList<Candle> candles, ArrayList<Signal> signals) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Signal signal : signals) {
            if (!signal.isEvaluated()) {
                continue;
            }

            int idx = signal.getIndex();
            if (idx < 0 || idx >= candles.size()) {
                continue;
            }

            Candle candle = candles.get(idx);
            LocalDateTime ldt = LocalDateTime.parse(candle.getDate(), formatter);
            double x = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime();

            // Entry - חץ ירוק למעלה עם טקסט "Entry"
            XYPointerAnnotation entryArrow = new XYPointerAnnotation(
                    "Entry", x, signal.getEntryPrice(), Math.PI / 2);
            entryArrow.setPaint(Color.GREEN.darker());
            entryArrow.setArrowPaint(Color.GREEN.darker());
            entryArrow.setFont(new Font("Arial", Font.BOLD, 12));
            plot.addAnnotation(entryArrow);

            // הוסף טקסט של WIN או LOSS בצבע מתאים
            String winLossText = signal.isWinSignal() ? "WIN" : "LOSS";
            Color winLossColor = signal.isWinSignal() ? Color.GREEN.darker() : Color.RED.darker();

            // טקסט מעל חץ כניסה - מידע מפורט כולל WIN/LOSS
            String entryInfo = String.format("Entry: %.2f | TP: %.2f | Stop: %.2f | %s",
                    signal.getEntryPrice(), signal.getTpPrice(), signal.getStopPrice(), winLossText);
            XYTextAnnotation entryText = new XYTextAnnotation(entryInfo, x, signal.getEntryPrice() + 0.5);
            entryText.setFont(new Font("Arial", Font.PLAIN, 10));
            entryText.setPaint(winLossColor);
            entryText.setTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_CENTER);
            plot.addAnnotation(entryText);

            // Stop Loss - חץ אדום למטה עם טקסט "Stop"
            XYPointerAnnotation stopArrow = new XYPointerAnnotation(
                    "Stop", x, signal.getStopPrice(), -Math.PI / 2);
            stopArrow.setPaint(Color.RED.darker());
            stopArrow.setArrowPaint(Color.RED.darker());
            stopArrow.setFont(new Font("Arial", Font.BOLD, 12));
            plot.addAnnotation(stopArrow);



            // Take Profit - חץ ירוק למעלה עם טקסט "TP"
            XYPointerAnnotation tpArrow = new XYPointerAnnotation(
                    "TP", x, signal.getTpPrice(), Math.PI / 2);
            tpArrow.setPaint(Color.GREEN.darker());
            tpArrow.setArrowPaint(Color.GREEN.darker());
            tpArrow.setFont(new Font("Arial", Font.BOLD, 12));
            plot.addAnnotation(tpArrow);

            // אפשר להוסיף טקסט ל-TP במידת הצורך
        }
    }

    /**
     * Adds a performance summary subtitle to the chart.
     */
    public static void addPerformanceSubtitle(JFreeChart chart, Performance perf) {
        TextTitle perfTitle = new TextTitle(perf.toString(), PERFORMANCE_FONT);
        perfTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        chart.addSubtitle(perfTitle);
    }

    /**
     * Creates a line renderer with given color and stroke width.
     */
    public static XYLineAndShapeRenderer createLineRenderer(Color color, float strokeWidth) {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(strokeWidth));
        return renderer;
    }

    /**
     * Adds an SMA overlay to the given candle plot at a specific dataset index.
     */
    public static void addSMAtoCandlePlot(XYPlot candlePlot, ArrayList<Candle> candles, int period, int datasetIndex, Color color) {
        TimeSeries smaSeries = DatasetFactory.createSMASeries(candles, period);
        TimeSeriesCollection dataset = new TimeSeriesCollection(smaSeries);

        candlePlot.setDataset(datasetIndex, dataset);
        candlePlot.setRenderer(datasetIndex, createLineRenderer(color, DEFAULT_SMA_STROKE));
    }

    // --- Helper Methods ---
    private static double getTimeAsMilliseconds(String dateStr) {
        LocalDateTime ldt = LocalDateTime.parse(dateStr, FORMATTER);
        Date date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        return new Millisecond(date).getFirstMillisecond();
    }

    private static double getAnnotationY(Candle candle) {
        return Math.max(candle.getOpen(), candle.getClose()) + candle.getBodyHeight() * 0.3;
    }

    public static void resetToLastNCandles(XYPlot plot, ArrayList<Candle> candles, int N) {
        if (candles.size() < N) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Get time range of last N candles
        LocalDateTime startLDT = LocalDateTime.parse(candles.get(candles.size() - N).getDate(), formatter);
        LocalDateTime endLDT = LocalDateTime.parse(candles.get(candles.size() - 1).getDate(), formatter);

        long start = Date.from(startLDT.atZone(ZoneId.systemDefault()).toInstant()).getTime();
        long end = Date.from(endLDT.atZone(ZoneId.systemDefault()).toInstant()).getTime();

        // Apply range to domain axis (X axis)
        plot.getDomainAxis().setRange(start, end);
    }

}
