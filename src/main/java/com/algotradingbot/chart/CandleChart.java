package com.algotradingbot.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor; // Make sure this is imported
import java.awt.Dimension; // Optional for styling
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultHighLowDataset;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Performance;
import com.algotradingbot.core.Signal;

public class CandleChart extends JFrame {

    private static final Dimension DEFAULT_PANEL_SIZE = new Dimension(1600, 800);

    public CandleChart(String title, ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        super(title);
        setLayout(new BorderLayout());

        DefaultHighLowDataset dataset = DatasetFactory.createDataset(candles);
        JFreeChart chart = createCombinedChart(dataset, candles, perf); // <- שינוי כאן
        CombinedDomainXYPlot combinedPlot = (CombinedDomainXYPlot) chart.getPlot();
        XYPlot candlePlot = (XYPlot) combinedPlot.getSubplots().get(0); // קבל את גרף הנרות
        candlePlot.setBackgroundPaint(Color.WHITE);
        candlePlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        candlePlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        candlePlot.setDomainPannable(true);  // מאפשר פאן בציר X (זמן)
        candlePlot.setRangePannable(true);   // מאפשר פאן בציר Y (מחיר)

        //AnnotationUtils.addSignalAnnotations(candlePlot, candles, signals); // תעביר לו את זה
        AnnotationUtils.addTradeAnnotations(candlePlot, candles, signals);

        ChartPanel chartPanel = createInteractivePanel(chart, candlePlot, candles);
        add(chartPanel, BorderLayout.CENTER);

    }

    private JFreeChart createCombinedChart(DefaultHighLowDataset candleDataset, ArrayList<Candle> candles, Performance perf) {
        int visibleCount = 1000;
        int startIndex = Math.max(0, candles.size() - visibleCount);
        int endIndex = candles.size() - 1;

        NumberAxis priceAxis = createPriceAxis(candles, startIndex, endIndex);
        XYPlot candlePlot = PlotFactory.createCandlePlot(candleDataset, priceAxis);

        XYPlot volumePlot = PlotFactory.createVolumePlot(candles);

        DateAxis timeAxis = createTimeAxis(candles, startIndex, endIndex);
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
        combinedPlot.add(candlePlot, 5);
        combinedPlot.add(volumePlot, 1);
        combinedPlot.setDomainPannable(true);
        combinedPlot.setBackgroundPaint(Color.WHITE);
        combinedPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        combinedPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, false);
        AnnotationUtils.addPerformanceSubtitle(chart, perf);
        stylePriceAxis(priceAxis);

       
        //StrategyChartUtils.TrendRSIBBBandUtils(candlePlot, combinedPlot, candles, 1);
        StrategyChartUtils.BBbandWithComma(candlePlot, candles, 1);
        return chart;
    }

    private NumberAxis createPriceAxis(ArrayList<Candle> candles, int start, int end) {
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (int i = start; i <= end; i++) {
            Candle c = candles.get(i);
            min = Math.min(min, c.getLow());
            max = Math.max(max, c.getHigh());
        }
        double padding = (max - min) * 0.02;
        NumberAxis axis = new NumberAxis("Price");
        axis.setAutoRange(false);
        axis.setRange(min - padding, max + padding);
        axis.setAutoRangeIncludesZero(false);
        axis.setAutoRangeStickyZero(false);
        axis.setLowerMargin(0.05);
        axis.setUpperMargin(0.05);
        return axis;
    }

    private DateAxis createTimeAxis(ArrayList<Candle> candles, int start, int end) {
        DateAxis axis = new DateAxis("Time");
        axis.setAutoRange(false);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime startLdt = LocalDateTime.parse(candles.get(start).getDate(), formatter);
        LocalDateTime endLdt = LocalDateTime.parse(candles.get(end).getDate(), formatter);

        axis.setRange(
                Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant())
        );

        long totalMinutes = Math.max(1, java.time.Duration.between(startLdt, endLdt).toMinutes());
        int targetLabelCount = 50;
        long minutesPerTick = Math.max(1, totalMinutes / targetLabelCount);

        int tickCount;
        if (minutesPerTick < 60) {
            tickCount = Math.max(1, (int) minutesPerTick);
            axis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, tickCount));
            axis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        } else if (minutesPerTick < 1440) {
            tickCount = Math.max(1, (int) (minutesPerTick / 60));
            axis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, tickCount));
            axis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH"));
        } else if (minutesPerTick < 43200) { // פחות מ-30 יום
            tickCount = Math.max(1, (int) (minutesPerTick / 1440));
            axis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, tickCount));
            axis.setDateFormatOverride(new SimpleDateFormat("dd/MM"));
        } else {
            tickCount = Math.max(1, (int) (minutesPerTick / 43200));
            axis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, tickCount));
            axis.setDateFormatOverride(new SimpleDateFormat("MM/yyyy"));
        }

        axis.setLowerMargin(0.01);
        axis.setUpperMargin(0.01);

        return axis;
    }

    private void stylePriceAxis(NumberAxis priceAxis) {
        priceAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        priceAxis.setLabelFont(new Font("Arial", Font.BOLD, 13));
        priceAxis.setTickLabelInsets(new org.jfree.chart.ui.RectangleInsets(2, 5, 2, 5));
        priceAxis.setLabelInsets(new org.jfree.chart.ui.RectangleInsets(5, 5, 5, 5));
    }

    public static ChartPanel createInteractivePanel(JFreeChart chart, XYPlot candlePlot, ArrayList<Candle> candles) {
        ChartPanel panel = new ChartPanel(chart);

        panel.setMouseWheelEnabled(true);
        panel.setMouseZoomable(true);
        panel.setDomainZoomable(true);
        panel.setRangeZoomable(true);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        panel.setPreferredSize(DEFAULT_PANEL_SIZE);

        // ✅ Reset on double-click
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    AnnotationUtils.resetToLastNCandles(candlePlot, candles, 1000);
                }
            }
        });

        return panel;
    }

    public static void showChart(ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        SwingUtilities.invokeLater(() -> {
            CandleChart chart = new CandleChart("Candlestick Chart", candles, signals, perf);
            chart.setSize(1000, 700);
            chart.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // ✅ רק סוגר את החלון הזה
            chart.setVisible(true);
        });
    }

}
