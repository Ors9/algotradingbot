package com.algotradingbot.chart;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class RendererFactory {

    public static CandlestickRenderer createCandleRenderer() {
        CandlestickRenderer renderer = new CandlestickRenderer();

        renderer.setUseOutlinePaint(true); // ✅ השתמש בצבע הקו
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);
        renderer.setUpPaint(new Color(0, 200, 0));      // גוף נר ירוק
        renderer.setDownPaint(new Color(200, 0, 0));    // גוף נר אדום
        renderer.setSeriesPaint(0, Color.BLACK);        // outline (כברירת מחדל)
        renderer.setDrawVolume(false);

        renderer.setSeriesOutlinePaint(0, null); // ננטרל את ברירת המחדל השחורה אם צריך

        return renderer;
    }

    public static XYBarRenderer createVolumeRenderer() {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new org.jfree.chart.renderer.xy.StandardXYBarPainter());
        renderer.setSeriesPaint(0, new Color(0, 102, 204));
        return renderer;
    }

    public static XYLineAndShapeRenderer createLineRenderer(Color color, float width) {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(width));
        return renderer;
    }
}
