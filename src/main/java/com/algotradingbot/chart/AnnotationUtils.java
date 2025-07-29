package com.algotradingbot.chart;

import java.awt.Color;
import java.awt.Font;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.Millisecond;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;

public class AnnotationUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void addSignalAnnotations(XYPlot plot, ArrayList<Candle> candles, ArrayList<Signal> signals) {
        for (Signal signal : signals) {
            if (!signal.isEvaluated()) {
                continue;
            }
            int index = signal.getIndex();
            if (index < 0 || index >= candles.size()) {
                continue;
            }

            Candle candle = candles.get(index);
            LocalDateTime ldt = LocalDateTime.parse(candle.getDate(), FORMATTER); // ✅ חסר אצלך
            Date date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            double x = new Millisecond(date).getFirstMillisecond();

            double y = Math.max(candle.getOpen(), candle.getClose()) + candle.getBodyHeight() * 0.3;

            String marker = "↓";
            Color color = signal.isWinSignal() ? Color.GREEN : Color.RED;

            XYTextAnnotation annotation = new XYTextAnnotation(marker, x, y);
            annotation.setFont(new Font("Arial", Font.BOLD, 25));
            annotation.setPaint(color);
            annotation.setTextAnchor(TextAnchor.CENTER);

            plot.addAnnotation(annotation);
        }
    }
}
