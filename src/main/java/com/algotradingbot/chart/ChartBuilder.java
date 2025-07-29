package com.algotradingbot.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Performance;
import com.algotradingbot.core.Signal;

public class ChartBuilder {

    public static JFreeChart buildFullChart(ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        // יצירת תתי גרפים (נרות, ווליום, RSI)
        XYPlot candlePlot = PlotFactory.createCandlePlot(candles);

        XYPlot volumePlot = PlotFactory.createVolumePlot(candles);
        XYPlot rsiPlot = PlotFactory.createRSIPlot(candles);

        // ציר זמן משותף
        DateAxis timeAxis = new DateAxis("Time");
        timeAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        timeAxis.setAutoRange(true);                      // ✅ זה עובר למעלה
        timeAxis.setAutoTickUnitSelection(true);   // ✅ מאפשר שינוי מרווחים עם Zoom
        timeAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE); // אופציונלי
        timeAxis.setLowerMargin(0.05);
        timeAxis.setUpperMargin(0.05);
        timeAxis.setTickLabelsVisible(true);
        timeAxis.setVerticalTickLabels(true); // אם התאריכים עולים אחד על השני

        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
        combinedPlot.add(candlePlot, 5);
        combinedPlot.add(volumePlot, 1);
        combinedPlot.add(rsiPlot, 2);
        combinedPlot.setBackgroundPaint(Color.WHITE);

        // הגרף הראשי
        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, false);

        // הוספת טקסט ביצועים
        addPerformanceTitles(chart, perf);

        // חצים של כניסות
        AnnotationUtils.addSignalAnnotations(candlePlot, candles, signals);

        return chart;
    }

    private static void addPerformanceTitles(JFreeChart chart, Performance perf) {
        TextTitle title = new TextTitle("Performance Summary");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setPaint(new Color(50, 50, 50));
        title.setHorizontalAlignment(HorizontalAlignment.CENTER);
        title.setPadding(new org.jfree.chart.ui.RectangleInsets(5, 10, 5, 10));

        TextTitle details = new TextTitle(perf.toString());
        details.setFont(new Font("Arial", Font.PLAIN, 12));
        details.setPaint(new Color(80, 80, 80));
        details.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        details.setPadding(new org.jfree.chart.ui.RectangleInsets(0, 10, 5, 10));

        chart.addSubtitle(title);
        chart.addSubtitle(details);
    }

    public static ChartPanel createInteractivePanel(JFreeChart chart) {
        CombinedDomainXYPlot combinedPlot = (CombinedDomainXYPlot) chart.getPlot();
        combinedPlot.setDomainPannable(true);

        for (Object subplot : combinedPlot.getSubplots()) {
            if (subplot instanceof XYPlot) {
                ((XYPlot) subplot).setDomainPannable(true);
            }
        }

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);   // ✅ חובה - מאפשר zoom עם גלגלת
        panel.setMouseZoomable(true);       // ✅ חובה - מפעיל zoom על mouse wheel

        // לא נוגע בפאנינג, לא ב-Range
        panel.setDomainZoomable(true);      // Zoom בציר הזמן (X)
        panel.setRangeZoomable(false);      // לא צריך בציר Y

        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        panel.setPreferredSize(new Dimension(1600, 800));
        return panel;
    }
    
}
