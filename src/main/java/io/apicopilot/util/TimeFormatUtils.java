package io.apicopilot.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeFormatUtils {

    private static final DateTimeFormatter MONTH_DAY =
            DateTimeFormatter.ofPattern("MMM d");           // Apr 3

    private static final DateTimeFormatter MONTH_DAY_YEAR =
            DateTimeFormatter.ofPattern("MMM d, yyyy");     // Apr 3, 2025

    public static String formatRelativeTime(Instant time) {
        return formatRelativeTime(time, ZoneId.systemDefault());
    }

    public static String formatRelativeTime(Instant time, ZoneId zone) {
        if (time == null) return "";

        Instant now = Instant.now();
        long seconds = Duration.between(time, now).getSeconds();

        if (seconds < 0) {
            // 未来时间，直接返回日期
            return formatDate(time, zone);
        }

        // < 1 min
        if (seconds < 60) {
            return "just now";
        }

        // < 1 hour
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }

        // < 24 hours
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }

        // < 7 days
        long days = hours / 24;
        if (days < 7) {
            return days + "d ago";
        }

        // >= 7 days → 显示日期
        return formatDate(time, zone);
    }

    private static String formatDate(Instant time, ZoneId zone) {
        ZonedDateTime zdt = time.atZone(zone);
        ZonedDateTime now = ZonedDateTime.now(zone);

        if (zdt.getYear() == now.getYear()) {
            return zdt.format(MONTH_DAY);         // Apr 3
        } else {
            return zdt.format(MONTH_DAY_YEAR);    // Apr 3, 2025
        }
    }
}