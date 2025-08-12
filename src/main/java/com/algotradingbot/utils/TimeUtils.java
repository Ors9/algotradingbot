package com.algotradingbot.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.algotradingbot.core.Candle;

public class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ---------- תוספות לטיפול בפורמט IB ----------
    private static final ZoneId IB_ZONE = ZoneId.of("US/Eastern");
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter IB_WITH_ZONE = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss VV");
    private static final DateTimeFormatter IB_NO_ZONE = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    private static final DateTimeFormatter IB_DATE_ONLY = DateTimeFormatter.BASIC_ISO_DATE; // "yyyyMMdd"

    private static final int[][] TRADING_SESSIONS = {
        {0, 11}, // לילה עד בוקר
        {11, 13}, // בוקר מאוחר
        {13, 15}, // צהריים
        //{15, 20},  // אחר צהריים עד ערב
        {20, 24} // לילה
    };

    /**
     * ממיר מחרוזת IB ל-LocalDateTime באזור הזמן המקומי (שומר את ה- instant
     * נכון).
     */
    public static LocalDateTime parseIb(String ibStr) {
        String s = ibStr.trim();
        // עם אזור זמן, למשל "20240814 20:00:00 US/Eastern"
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(s, IB_WITH_ZONE);
            return zdt.withZoneSameInstant(LOCAL_ZONE).toLocalDateTime();
        } catch (Exception ignore) {
        }

        // בלי אזור זמן: נניח IB_ZONE
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, IB_NO_ZONE);
            return ldt.atZone(IB_ZONE).withZoneSameInstant(LOCAL_ZONE).toLocalDateTime();
        } catch (Exception ignore) {
        }

        // תאריך יומי בלבד "yyyyMMdd"
        LocalDate ld = LocalDate.parse(s, IB_DATE_ONLY);
        return ld.atStartOfDay(IB_ZONE).withZoneSameInstant(LOCAL_ZONE).toLocalDateTime();
    }

    /**
     * ממיר פורמט IB למחרוזת בפורמט הישן "dd/MM/yyyy HH:mm" (נוח לשמירה/תצוגה).
     */
    public static String ibToLegacyString(String ibStr) {
        LocalDateTime ldtLocal = parseIb(ibStr);
        return ldtLocal.format(FORMATTER);
    }

    /**
     * parse "חכם": מנסה קודם פורמט ישן, ואם נכשל — פורמט IB.
     */
    public static LocalDateTime parseFlexible(String dateStr) {
        try {
            return parse(dateStr); // הישן
        } catch (Exception e) {
            return parseIb(dateStr); // IB
        }
    }

    /**
     * גרסאות "חכמות" לפונקציות קיימות, לא מחליפות את הישנות:
     */
    public static boolean isSaturdayFlexible(String dateStr) {
        return parseFlexible(dateStr).getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    public static boolean isSundayFlexible(String dateStr) {
        return parseFlexible(dateStr).getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static boolean isTradingHourFlexible(String dateStr) {
        int hour = parseFlexible(dateStr).getHour();
        for (int[] session : TRADING_SESSIONS) {
            if (hour >= session[0] && hour < session[1]) {
                return true;
            }
        }
        return false;
    }
    // ---------- סוף התוספות ----------

    // ===== הקוד הישן נשאר כמות שהוא =====
    public static boolean isSaturday(String dateStr) {
        return parse(dateStr).getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    public static boolean isSunday(String dateStr) {
        return parse(dateStr).getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static boolean isTradingHour(String dateStr) {
        int hour = parse(dateStr).getHour();
        for (int[] session : TRADING_SESSIONS) {
            if (hour >= session[0] && hour < session[1]) {
                return true;
            }
        }
        return false;
    }

    public static LocalDateTime parse(String dateStr) {
        return LocalDateTime.parse(dateStr, FORMATTER);
    }

    public static boolean isFxTradableInstant(long epochMs) {
        ZonedDateTime z = Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("US/Eastern"));
        DayOfWeek d = z.getDayOfWeek();
        int hour = z.getHour();

        // סגור בשבת
        if (d == DayOfWeek.SATURDAY) {
            return false;
        }
        // סגור בראשון עד 17:00 ET
        if (d == DayOfWeek.SUNDAY && hour < 17) {
            return false;
        }
        // סגור בשישי מ-17:00 ET
        if (d == DayOfWeek.FRIDAY && hour >= 17) {
            return false;
        }

        return true;
    }

    /**
     * Compress display time to a continuous hourly sequence (no weekend gap on
     * axis).
     */
    public static ArrayList<Candle> compressToSequentialHours(ArrayList<Candle> candles) {
        ArrayList<Candle> out = new ArrayList<>(candles.size());
        if (candles.isEmpty()) {
            return out;
        }

        // keep your legacy display format so CandleChart parsing stays unchanged
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        ZoneId zone = ZoneId.systemDefault();

        // start from the first candle’s local time (keep its minute to avoid visual jump if :15)
        Instant firstInst = Instant.ofEpochMilli(candles.get(0).getDateMillis());
        LocalDateTime t = LocalDateTime.ofInstant(firstInst, zone).withSecond(0).withNano(0);

        for (Candle c : candles) {
            String newDate = t.format(fmt);
            out.add(new Candle(newDate, c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()));
            t = t.plusHours(1); // continuous hourly steps (no weekend hole)
        }
        return out;
    }
}
