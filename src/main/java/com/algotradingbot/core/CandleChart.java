package com.algotradingbot.core;

import java.awt.BorderLayout;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;

public class CandleChart extends JFrame {

    public CandleChart(String title, ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        super(title);
        setLayout(new BorderLayout());

        DefaultHighLowDataset dataset = createDataset(candles);
        JFreeChart chart = createCombinedChart(dataset, candles, perf); // <- שינוי כאן
        CombinedDomainXYPlot combinedPlot = (CombinedDomainXYPlot) chart.getPlot();
        XYPlot candlePlot = (XYPlot) combinedPlot.getSubplots().get(0); // קבל את גרף הנרות

        addSignalAnnotations(candlePlot, candles, signals); // תעביר לו את זה

        JScrollPane scrollPane = new JScrollPane();
        ChartPanel chartPanel = createInteractivePanel(chart, scrollPane);
        scrollPane.setViewportView(chartPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);
        enableScrollWithMouseDrag(chartPanel, scrollPane);
    }

    private JFreeChart createCombinedChart(DefaultHighLowDataset candleDataset, ArrayList<Candle> candles, Performance perf) {
        // --- גרף נרות ---
        CandlestickRenderer candleRenderer = new CandlestickRenderer();
        candleRenderer.setDrawVolume(false); // ✅ מונע ציור עמודות ווליום בתוך גרף הנרות
        NumberAxis priceAxis = new NumberAxis("Price");
        priceAxis.setAutoRange(true);                     // ✅ יתאים את עצמו אוטומטית
        priceAxis.setAutoRangeIncludesZero(false);        // ❌ לא חייב לכלול 0 בציר
        XYPlot candlePlot = new XYPlot(candleDataset, null, priceAxis, candleRenderer);
        // --- גרף Volume ---
        TimeSeries series = new TimeSeries(""); // <-- אין כותרת מיותרת
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Candle c : candles) {
            LocalDateTime ldt = LocalDateTime.parse(c.getDate(), formatter);
            series.add(new Minute(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())), c.getVolume());
        }
        TimeSeriesCollection volumeDataset = new TimeSeriesCollection(series);
        XYBarRenderer volumeRenderer = new XYBarRenderer();
        NumberAxis volumeAxis = new NumberAxis("Volume");
        XYPlot volumePlot = new XYPlot(volumeDataset, null, volumeAxis, volumeRenderer);

        // --- שילוב עם ציר זמן משותף ---
        DateAxis timeAxis = new DateAxis("Time");
        timeAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
        combinedPlot.add(candlePlot, 3); // משקל גדול יותר לנרות
        combinedPlot.add(volumePlot, 1); // משקל קטן ל-volume

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


        

        return chart;
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

    private ChartPanel createInteractivePanel(JFreeChart chart, JScrollPane scrollPane) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        panel.restoreAutoBounds(); // מחזיר את הזום לברירת מחדל

        // ביטול זום עם גרירה – מאפשר רק הזזה עם גרירת עכבר
        panel.setMouseZoomable(false);
        panel.setDomainZoomable(false);
        panel.setRangeZoomable(true);

        // כן להשאיר זום עם גלגלת
        panel.setMouseWheelEnabled(true);

        panel.setPreferredSize(new Dimension(2000, 700));
        return panel;
    }

    private void enableScrollWithMouseDrag(ChartPanel panel, JScrollPane scrollPane) {
        final int[] lastX = {-1};

        panel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (lastX[0] != -1) {
                    int dx = e.getX() - lastX[0];
                    JScrollBar hBar = scrollPane.getHorizontalScrollBar();
                    hBar.setValue(hBar.getValue() - dx);
                }
                lastX[0] = e.getX();
            }
        });

        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                lastX[0] = -1;
            }
        });
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
            chart.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            chart.setVisible(true);
        });
    }
}
