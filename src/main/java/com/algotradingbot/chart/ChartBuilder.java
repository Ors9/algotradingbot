package com.algotradingbot.chart;

import java.awt.Color;

import java.awt.Font;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleInsets;


import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Performance;
import com.algotradingbot.core.Signal;

public class ChartBuilder {

    // Constants
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font SUBTITLE_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Color TITLE_COLOR = new Color(50, 50, 50);
    private static final Color SUBTITLE_COLOR = new Color(80, 80, 80);

    private static final int CANDLE_WEIGHT = 5;
    private static final int VOLUME_WEIGHT = 1;
    private static final int RSI_WEIGHT = 2;

    

    public static JFreeChart buildFullChart(ArrayList<Candle> candles, ArrayList<Signal> signals, Performance perf) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candle list is empty or null.");
        }

        XYPlot candlePlot = PlotFactory.createCandlePlot(candles);
        XYPlot volumePlot = PlotFactory.createVolumePlot(candles);
        XYPlot rsiPlot = PlotFactory.createRSIPlot(candles);

        DateAxis timeAxis = createTimeAxis();

        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
        combinedPlot.add(candlePlot, CANDLE_WEIGHT);
        combinedPlot.add(volumePlot, VOLUME_WEIGHT);
        combinedPlot.add(rsiPlot, RSI_WEIGHT);
        combinedPlot.setBackgroundPaint(Color.WHITE);

        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, false);

        addPerformanceTitles(chart, perf);
        AnnotationUtils.addSignalAnnotations(candlePlot, candles, signals);

        return chart;
    }

    private static DateAxis createTimeAxis() {
        DateAxis axis = new DateAxis("Time");
        axis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        axis.setAutoRange(true);
        axis.setAutoTickUnitSelection(true);
        axis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        axis.setLowerMargin(0.05);
        axis.setUpperMargin(0.05);
        axis.setTickLabelsVisible(true);
        axis.setVerticalTickLabels(true);
        return axis;
    }

    private static void addPerformanceTitles(JFreeChart chart, Performance perf) {
        TextTitle title = new TextTitle("Performance Summary", TITLE_FONT);
        title.setPaint(TITLE_COLOR);
        title.setHorizontalAlignment(HorizontalAlignment.CENTER);
        title.setPadding(new RectangleInsets(5, 10, 5, 10));

        TextTitle details = new TextTitle(perf.toString(), SUBTITLE_FONT);
        details.setPaint(SUBTITLE_COLOR);
        details.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        details.setPadding(new RectangleInsets(0, 10, 5, 10));

        chart.addSubtitle(title);
        chart.addSubtitle(details);
    }


}
