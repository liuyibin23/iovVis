package org.thingsboard.server.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Created by ztao at 2019/1/2 16:32.
 */
public class DateAndTimeUtils {

    public static boolean isAtToday(long timestamp) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 0, 0, 0, 0);
        LocalDateTime end = start.plusDays(1);
        return isBetween(timestampToDateTime(timestamp), start, end);
    }

    public static boolean isInThisMonth(long timestamp) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime start = LocalDateTime.of(firstDay.getYear(), firstDay.getMonth(), firstDay.getDayOfMonth(), 0, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(lastDay.getYear(), lastDay.getMonth(), lastDay.getDayOfMonth(), 23, 59, 59, 0);
        return isBetween(timestampToDateTime(timestamp), start, end);
    }

    public static LocalDateTime timestampToDateTime(long milliSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliSeconds), ZoneId.systemDefault());
    }

    public static boolean isBetween(LocalDateTime time, LocalDateTime startTime, LocalDateTime endTime) {
        return time.isAfter(startTime) && time.isBefore(endTime);
    }

}
