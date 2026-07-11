package io.apix.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeFormatUtils {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");           // 2025-04-03

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");      // 2025-04-03 14:30

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

    public static String formatDateTime(Instant time) {
        return formatDateTime(time, ZoneId.systemDefault());
    }

    public static String formatDateTime(Instant time, ZoneId zone) {
        if (time == null) return "";
        return time.atZone(zone).format(DATE_TIME);
    }

    private static String formatDate(Instant time, ZoneId zone) {
        ZonedDateTime zdt = time.atZone(zone);

        return zdt.format(DATE);
    }
}
