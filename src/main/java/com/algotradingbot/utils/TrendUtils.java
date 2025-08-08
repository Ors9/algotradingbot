package com.algotradingbot.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.algotradingbot.core.Candle;

public class TrendUtils {

    public enum RSILevel {
        OVERSOLD(30),
        OVERBOUGHT(70),
        RSI_PERIOD(14);
        private final double value;

        RSILevel(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    public enum SMAType {
        SMA_20(20),
        SMA_50(50),
        SMA_100(100),
        SMA_200(200);

        private final int period;

        SMAType(int period) {
            this.period = period;
        }

        public int getPeriod() {
            return period;
        }
    }

    public enum EMAType {
        EMA_21(21),
        EMA_50(50),
        EMA_100(100),
        EMA_240(240);

        private final int period;

        EMAType(int period) {
            this.period = period;
        }

        public int getPeriod() {
            return period;
        }
    }

    public enum ADXLevel {
        FLAT(20.0),
        TRENDING(25.0),
        STRONG(40.0);

        private final double value;

        ADXLevel(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

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
            double sma20 = calculateSMA(candles, index, SMAType.SMA_20.getPeriod());
            double sma50 = calculateSMA(candles, index, SMAType.SMA_50.getPeriod());
            double sma100 = calculateSMA(candles, index, SMAType.SMA_100.getPeriod());

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
            double sma20 = calculateSMA(candles, index, SMAType.SMA_20.getPeriod());
            double sma50 = calculateSMA(candles, index, SMAType.SMA_50.getPeriod());
            double sma100 = calculateSMA(candles, index, SMAType.SMA_100.getPeriod());

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

    public static Double calculateMFI(List<Candle> candles, int index, int period) {
        if (index < period) {
            return null;
        }

        double positiveFlow = 0;
        double negativeFlow = 0;

        for (int i = index - period + 1; i <= index; i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            // Typical Price = (High + Low + Close) / 3
            double currTypical = (curr.getHigh() + curr.getLow() + curr.getClose()) / 3.0;
            double prevTypical = (prev.getHigh() + prev.getLow() + prev.getClose()) / 3.0;

            double rawMoneyFlow = currTypical * curr.getVolume();

            if (currTypical > prevTypical) {
                positiveFlow += rawMoneyFlow;
            } else if (currTypical < prevTypical) {
                negativeFlow += rawMoneyFlow;
            }
            // if they are equal, ignore this period
        }

        if (positiveFlow + negativeFlow == 0) {
            return 50.0; // ניטרלי
        }
        double moneyFlowRatio = positiveFlow / negativeFlow;
        double mfi = 100 - (100 / (1 + moneyFlowRatio));

        return mfi;
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

    public static Double calculateEMAAtIndex(ArrayList<Candle> candles, int index, int period) {
        if (index < period) {
            return null;
        }

        try {
            // Step 1: SMA for first EMA value
            double sma = calculateSMA(candles, index - period, period);
            double ema = sma;
            double k = 2.0 / (period + 1);

            // Step 2: Loop up to current index
            for (int i = index - period + 1; i <= index; i++) {
                double price = candles.get(i).getClose();
                ema = price * k + ema * (1 - k);
            }

            return ema;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isHighTimeFrameCommaForPeriod(ArrayList<Candle> candles, int index, int period) {
        if (index < TrendUtils.EMAType.EMA_240.getPeriod() || index < period) {
            return false;
        }

        for (int i = index - period + 1; i <= index; i++) {
            Double ema21 = TrendUtils.calculateEMAAtIndex(candles, i, EMAType.EMA_21.getPeriod());
            Double ema50 = TrendUtils.calculateEMAAtIndex(candles, i, EMAType.EMA_50.getPeriod());
            Double ema100 = TrendUtils.calculateEMAAtIndex(candles, i, EMAType.EMA_100.getPeriod());
            Double ema240 = TrendUtils.calculateEMAAtIndex(candles, i, EMAType.EMA_240.getPeriod());
            double buffer = candles.get(i).getClose() * 0.005;

            if (ema21 == null || ema50 == null || ema100 == null || ema240 == null) {
                return false;
            }
            if (!(ema21 > ema50 + buffer && ema50 > ema100 + buffer && ema100 > ema240 + buffer)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isHighTimeFrameComma(ArrayList<Candle> candles, int index) {
        Double ema21 = calculateEMAAtIndex(candles, index, EMAType.EMA_21.getPeriod());
        Double ema50 = calculateEMAAtIndex(candles, index, EMAType.EMA_50.getPeriod());
        Double ema100 = calculateEMAAtIndex(candles, index, EMAType.EMA_100.getPeriod());
        Double ema240 = calculateEMAAtIndex(candles, index, EMAType.EMA_240.getPeriod());

        Candle c = candles.get(index);

        boolean valid = ema21 != null && ema50 != null && ema100 != null && ema240 != null
                && ema21 > ema50 && ema50 > ema100 && ema100 > ema240;

        return valid;
    }

    public static boolean isHighTimeFrameBearishComma(ArrayList<Candle> candles, int index) {

        Double ema21 = calculateEMAAtIndex(candles, index, EMAType.EMA_21.getPeriod());
        Double ema50 = calculateEMAAtIndex(candles, index, EMAType.EMA_50.getPeriod());
        Double ema100 = calculateEMAAtIndex(candles, index, EMAType.EMA_100.getPeriod());
        Double ema240 = calculateEMAAtIndex(candles, index, EMAType.EMA_240.getPeriod());

        return ema21 != null && ema50 != null && ema100 != null && ema240 != null
                && ema21 < ema50 && ema50 < ema100 && ema100 < ema240;
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
            return false;
        }

    }

    public static boolean isTouchingLowerBB(List<Candle> candles, int index, int period) {
        if (index < period - 1) {
            return false; // Not enough candles
        }

        BollingerBands bb = getBollingerBands(candles, index, period);
        if (bb == null) {
            return false;
        }

        Candle candle = candles.get(index);
        return candle.getLow() <= bb.getLower(); // נוגע או מתחת
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
        if (index < period * 2) {
            return null; // צריך מספיק נרות
        }

        List<Double> dxList = new ArrayList<>();

        for (int i = index - period + 1; i <= index; i++) {
            double tr = 0;
            double plusDM = 0;
            double minusDM = 0;

            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            double highDiff = curr.getHigh() - prev.getHigh();
            double lowDiff = prev.getLow() - curr.getLow();

            if (highDiff > lowDiff && highDiff > 0) {
                plusDM = highDiff;
            }
            if (lowDiff > highDiff && lowDiff > 0) {
                minusDM = lowDiff;
            }

            tr = Math.max(
                    curr.getHigh() - curr.getLow(),
                    Math.max(
                            Math.abs(curr.getHigh() - prev.getClose()),
                            Math.abs(curr.getLow() - prev.getClose())
                    )
            );

            double smoothedTR = 0;
            double smoothedPlusDM = 0;
            double smoothedMinusDM = 0;

            // חישוב ממוצע פשוט לאורך 'period' ימים אחורה
            for (int j = i - period + 1; j <= i; j++) {
                Candle c = candles.get(j);
                Candle p = candles.get(j - 1);

                double hd = c.getHigh() - p.getHigh();
                double ld = p.getLow() - c.getLow();

                smoothedPlusDM += (hd > ld && hd > 0) ? hd : 0;
                smoothedMinusDM += (ld > hd && ld > 0) ? ld : 0;

                double trj = Math.max(
                        c.getHigh() - c.getLow(),
                        Math.max(Math.abs(c.getHigh() - p.getClose()), Math.abs(c.getLow() - p.getClose()))
                );
                smoothedTR += trj;
            }

            double plusDI = 100 * (smoothedPlusDM / smoothedTR);
            double minusDI = 100 * (smoothedMinusDM / smoothedTR);

            if (plusDI + minusDI == 0) {
                dxList.add(0.0);
            } else {
                double dx = 100 * Math.abs(plusDI - minusDI) / (plusDI + minusDI);
                dxList.add(dx);
            }
        }

        // ADX = ממוצע של כל ה־DXים
        return dxList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static Double getPlusDI(List<Candle> candles, int index, int period) {
        if (index < period + 1) {
            return null;
        }
        double plusDmSum = 0;
        double trSum = 0;

        for (int i = index - period + 1; i <= index; i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            double highDiff = curr.getHigh() - prev.getHigh();
            double lowDiff = prev.getLow() - curr.getLow();

            double plusDM = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0;
            double tr = Math.max(curr.getHigh() - curr.getLow(),
                    Math.max(Math.abs(curr.getHigh() - prev.getClose()),
                            Math.abs(curr.getLow() - prev.getClose())));
            plusDmSum += plusDM;
            trSum += tr;
        }

        double atr = trSum / period;
        return 100 * (plusDmSum / period) / atr;
    }

    public static Double getMinusDI(List<Candle> candles, int index, int period) {
        if (index < period + 1) {
            return null;
        }

        double minusDmSum = 0;
        double trSum = 0;

        for (int i = index - period + 1; i <= index; i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            double highDiff = curr.getHigh() - prev.getHigh();
            double lowDiff = prev.getLow() - curr.getLow();

            double minusDM = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0;

            double tr = Math.max(
                    curr.getHigh() - curr.getLow(),
                    Math.max(
                            Math.abs(curr.getHigh() - prev.getClose()),
                            Math.abs(curr.getLow() - prev.getClose())
                    )
            );

            minusDmSum += minusDM;
            trSum += tr;
        }

        double atr = trSum / period;
        return 100 * (minusDmSum / period) / atr;
    }

    public static boolean isADXUptrend(List<Candle> candles, int index, int period) {
        Double adx = calculateADX(candles, index, period);
        Double plusDI = getPlusDI(candles, index, period);
        Double minusDI = getMinusDI(candles, index, period);

        if (adx == null || plusDI == null || minusDI == null) {

            return false;
        }

        if (adx < 20 || plusDI <= minusDI) {

            return false;
        }

        return true;
    }

    public static double calculateATR(List<Candle> candles, int index, int period) {
        if (index < period || candles == null || candles.size() <= index) {
            return -1; // לא ניתן לחשב ATR
        }

        double sumTR = 0.0;

        for (int i = index - period + 1; i <= index; i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            double highLow = curr.getHigh() - curr.getLow();
            double highClose = Math.abs(curr.getHigh() - prev.getClose());
            double lowClose = Math.abs(curr.getLow() - prev.getClose());

            double trueRange = Math.max(highLow, Math.max(highClose, lowClose));
            sumTR += trueRange;
        }

        return sumTR / period; // ממוצע של ה־True Range
    }

}
