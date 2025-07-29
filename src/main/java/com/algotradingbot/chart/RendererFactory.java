package com.algotradingbot.chart;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class RendererFactory {

    public static final Color GREEN_CANDLE = new Color(0, 200, 0);
    public static final Color RED_CANDLE = new Color(200, 0, 0);
    public static final Color VOLUME_BAR = new Color(0, 102, 204);
    public static final Color RSI_LINE = Color.ORANGE;
    public static final Color BB_LINE = Color.GRAY;
    public static final Color BB_MIDDLE_LINE = Color.BLACK;

    public static CandlestickRenderer createCandleRenderer() {
        CandlestickRenderer renderer = new CandlestickRenderer();
        renderer.setUseOutlinePaint(true);
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);
        renderer.setUpPaint(GREEN_CANDLE);
        renderer.setDownPaint(RED_CANDLE);
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setDrawVolume(false);
        renderer.setSeriesOutlinePaint(0, null);
        return renderer;
    }

    public static XYBarRenderer createVolumeRenderer() {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new org.jfree.chart.renderer.xy.StandardXYBarPainter());
        renderer.setSeriesPaint(0, VOLUME_BAR);
        return renderer;
    }

    public static XYLineAndShapeRenderer createLineRenderer(Color color, float width) {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(width));
        return renderer;
    }
}

