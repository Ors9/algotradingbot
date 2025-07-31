package com.algotradingbot.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.algotradingbot.core.Candle;

public class TrendUtils {

    public static final double ADX_FLAT_THRESHOLD = 20.0;      // שוק מדשדש (רוב הזמן)
    public static final double ADX_TRENDING_THRESHOLD = 25.0;  // התחלה של מגמה
    public static final double ADX_STRONG_TREND = 40.0;        // מגמה חזקה מאוד

    public static final double RSI_OVERSOLD = 30.0;
    public static final double RSI_OVERBOUGHT = 70.0;

    public static boolean isShortTermUptrendHolding(ArrayList<Candle> candles, int index, int smaShortPeriod, int smaMidPeriod, int smaLargePeriod) {
        try {
            double smaShort = calculateSMA(candles, index, smaShortPeriod); // e.g., 20
            double smaMid = calculateSMA(candles, index, smaMidPeriod);     // e.g., 50
            //double smaLarge = calculateSMA(candles, index, smaLargePeriod); // e.g., 200

            // מגמה שורית פשוטה: SMA20 > SMA50 > SMA200
            //&& smaMid > smaLarge leave for now
            return smaShort > smaMid;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isStrongBullMarket(ArrayList<Candle> candles, int index, int sma20, int sma50, int sma200) {
        try {
            double sma200Val = calculateSMA(candles, index, sma200);
            double sma50Val = calculateSMA(candles, index, sma50);
            double sma20Val = calculateSMA(candles, index, sma20);
            double price = candles.get(index).getClose();

            return sma20Val > sma50Val || (sma50Val > sma200Val && price > sma20Val);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBullishEnough(ArrayList<Candle> candles, int index) {
        try {
            double close = candles.get(index).getClose();
            double sma20 = calculateSMA(candles, index, 20);
            double sma50 = calculateSMA(candles, index, 50);
            double sma100 = calculateSMA(candles, index, 100);

            int countAbove = 0;
            if (close > sma20 * 0.99) {
                countAbove++;
            }
            if (close > sma50 * 0.99) {
                countAbove++;
            }
            if (close > sma100 * 0.99) {
                countAbove++;
            }

            // דרוש לפחות 2 מתוך 3
            return countAbove >= 2;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBearishEnough(ArrayList<Candle> candles, int index) {
        try {
            double close = candles.get(index).getClose();
            double sma20 = calculateSMA(candles, index, 20);
            double sma50 = calculateSMA(candles, index, 50);
            double sma100 = calculateSMA(candles, index, 100);

            int countBelow = 0;
            if (close < sma20 * 1.01) {
                countBelow++;
            }
            if (close < sma50 * 1.01) {
                countBelow++;
            }
            if (close < sma100 * 1.01) {
                countBelow++;
            }

            // דרוש לפחות 2 מתוך 3
            return countBelow >= 2;
        } catch (Exception e) {
            return false;
        }
    }

    public static double averageVolume(ArrayList<Candle> candles, int startIndex, int period) {
        if (startIndex - period + 1 < 0) {
            throw new IllegalArgumentException("Not enough candles for volume average");
        }

        double sum = 0;
        for (int i = startIndex - period + 1; i <= startIndex; i++) {
            sum += candles.get(i).getVolume(); // בהנחה שיש getter ל-volume
        }
        return sum / period;
    }

    public static double averageRSI(ArrayList<Candle> candles, int startIndex, int period) {
        if (startIndex - period + 1 < 0) {
            throw new IllegalArgumentException("Not enough candles for RSI average");
        }

        double sum = 0;
        for (int i = startIndex - period + 1; i <= startIndex; i++) {
            sum += calculateRSI(candles, i, 14); // או כל תקופת RSI שתרצה
        }
        return sum / period;
    }

    public static double calculateSMA(ArrayList<Candle> candles, int index, int period) throws Exception {
        if (index - period + 1 < 0) {
            throw new IllegalArgumentException("Not enough candles");
        }

        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += candles.get(i).getClose();
        }
        return sum / period;
    }

    public static boolean isHighTFBullTrend(Candle candle, String symbol, int sma20Period, int sma50Period, int sma200Period) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime candleTime = LocalDateTime.parse(candle.getDate(), formatter);
            TFtrendAnalyzer tfa = new TFtrendAnalyzer(candleTime, symbol, "1d");

            double sma200 = tfa.smaCalc(sma200Period);
            double sma50 = tfa.smaCalc(sma50Period);
            double sma20 = tfa.smaCalc(sma20Period);
            double price = candle.getClose();

            return sma20 > sma50 && sma50 > sma200 && price > sma20;

        } catch (Exception e) {
            System.err.println("High TF trend check failed: " + e.getMessage());
            return false;
        }

    }

    public static BollingerBands getBollingerBands(List<Candle> candles, int index, int period) {
        if (index < period - 1) {
            return null; // Not enough candles
        }

        double sma;
        try {
            sma = calculateSMA(new ArrayList<>(candles), index, period); // נדרש cast כי calculateSMA מקבלת ArrayList
        } catch (Exception e) {
            return null;
        }

        double variance = 0;
        for (int i = index - period + 1; i <= index; i++) {
            double diff = candles.get(i).getClose() - sma;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / period);

        double upper = sma + 2 * stdDev;
        double lower = sma - 2 * stdDev;

        return new BollingerBands(sma, upper, lower);
    }

    public static Double calculateRSI(List<Candle> candles, int index, int period) {
        if (index < period) {
            return null;
        }

        double gain = 0;
        double loss = 0;

        for (int i = index - period + 1; i <= index; i++) {
            double change = candles.get(i).getClose() - candles.get(i - 1).getClose();
            if (change > 0) {
                gain += change;
            } else {
                loss -= change; // הופך ל־חיובי
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        if (avgLoss == 0) {
            return 100.0; // אין ירידות → RSI מקסימלי
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    public static Double calculateADX(List<Candle> candles, int index, int period) {
        if (index < period + 1) {
            return null; // צריך מספיק נרות גם לחישוב ממוצעים
        }

        double trSum = 0;
        double plusDmSum = 0;
        double minusDmSum = 0;
        List<Double> dxList = new ArrayList<>();

        // נצבור TR, +DM, -DM ל־period ימים
        for (int i = index - period + 1; i <= index; i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            double highDiff = curr.getHigh() - prev.getHigh();
            double lowDiff = prev.getLow() - curr.getLow();

            double plusDM = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0;
            double minusDM = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0;

            double tr = Math.max(
                    curr.getHigh() - curr.getLow(),
                    Math.max(
                            Math.abs(curr.getHigh() - prev.getClose()),
                            Math.abs(curr.getLow() - prev.getClose())
                    )
            );

            trSum += tr;
            plusDmSum += plusDM;
            minusDmSum += minusDM;
        }

        double atr = trSum / period;
        double plusDI = 100 * (plusDmSum / period) / atr;
        double minusDI = 100 * (minusDmSum / period) / atr;

        if (plusDI + minusDI == 0) {
            return 0.0;
        }

        double dx = 100 * Math.abs(plusDI - minusDI) / (plusDI + minusDI);

        // נחזיר פשוט את dx כמייצג ADX. אפשר לשפר בהמשך ל־EMA של DX לאורך זמן.
        return dx;
    }

}
