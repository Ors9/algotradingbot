package com.algotradingbot.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int[][] TRADING_SESSIONS = {
        {0, 11}, // לילה עד בוקר
        {11, 13}, // בוקר מאוחר
        {13, 15}, // צהריים
        //{15, 20},  // אחר צהריים עד ערב
        {20, 24} // לילה
    };

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

}
