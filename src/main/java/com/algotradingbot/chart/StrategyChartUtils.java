package com.algotradingbot.chart;

import java.awt.Color;
import java.util.ArrayList;

import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;

import com.algotradingbot.core.Candle;
import com.algotradingbot.utils.BollingerBands;
import com.algotradingbot.utils.TrendUtils;

public class StrategyChartUtils {

    /**
     * אסטרטגיה: Trend + RSI + Bollinger + SMA
     */
    public static void TrendRSIBBBandUtils(XYPlot candlePlot, CombinedDomainXYPlot combinedPlot,
            ArrayList<Candle> candles, int smaStartIndex) {
        int index = smaStartIndex;

        // Add SMA
        AnnotationUtils.addSMAtoCandlePlot(candlePlot, candles, TrendUtils.SMAType.SMA_20.getPeriod(), index++, Color.BLUE);
        AnnotationUtils.addSMAtoCandlePlot(candlePlot, candles, TrendUtils.SMAType.SMA_50.getPeriod(), index++, Color.GREEN);
        AnnotationUtils.addSMAtoCandlePlot(candlePlot, candles, TrendUtils.SMAType.SMA_200.getPeriod(), index++, Color.RED);

        // Add Bollinger Bands
        AnnotationUtils.addBollingerBands(candlePlot, candles, BollingerBands.BBPeriod.BB_20.getPeriod(), index++);

        // Add RSI
        XYPlot rsiPlot = PlotFactory.createRSIPlot(candles, (int) TrendUtils.RSILevel.RSI_PERIOD.getValue());
        combinedPlot.add(rsiPlot, 2);
    }

    /**
     * אסטרטגיה: BB + 4 EMA ("Comma")
     */
    public static void BBbandWithComma(XYPlot candlePlot, ArrayList<Candle> candles, int startIndex) {
        int index = startIndex;

        // Add 4 EMAs
        AnnotationUtils.addEMAtoCandlePlot(candlePlot, candles, TrendUtils.EMAType.EMA_21.getPeriod(), index++, Color.MAGENTA);
        AnnotationUtils.addEMAtoCandlePlot(candlePlot, candles, TrendUtils.EMAType.EMA_50.getPeriod(), index++, Color.ORANGE);
        AnnotationUtils.addEMAtoCandlePlot(candlePlot, candles, TrendUtils.EMAType.EMA_100.getPeriod(), index++, Color.CYAN);
        AnnotationUtils.addEMAtoCandlePlot(candlePlot, candles, TrendUtils.EMAType.EMA_240.getPeriod(), index++, Color.PINK);

        // Add Bollinger Bands
        AnnotationUtils.addBollingerBands(candlePlot, candles, BollingerBands.BBPeriod.BB_22.getPeriod(), index++);



    }



}
