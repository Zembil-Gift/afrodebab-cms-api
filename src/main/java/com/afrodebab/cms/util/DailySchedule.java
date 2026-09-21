package com.afrodebab.cms.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * "Every day / every N days at HH:mm in zone Z" evaluated against UTC instants. The local time
 * is converted to UTC per calendar day (never once up front), so DST shifts are handled; a
 * local time skipped by a DST gap runs at the shifted-forward instant.
 */
public final class DailySchedule {
    private DailySchedule() {}

    /** The last instant at or before {@code now} when the local {@code time} occurred in {@code zone}. */
    public static Instant latestOccurrence(LocalTime time, ZoneId zone, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant todayAt = ZonedDateTime.of(today, time, zone).toInstant();
        return todayAt.isAfter(now) ? ZonedDateTime.of(today.minusDays(1), time, zone).toInstant() : todayAt;
    }

    /** The first instant strictly after {@code now} when the local {@code time} occurs in {@code zone}. */
    public static Instant nextOccurrence(LocalTime time, ZoneId zone, Instant now) {
        LocalDate latestDate = LocalDate.ofInstant(latestOccurrence(time, zone, now), zone);
        return ZonedDateTime.of(latestDate.plusDays(1), time, zone).toInstant();
    }

    /** Daily job: due when it has not run since the latest occurrence (so a missed run catches up). */
    public static boolean isDailyDue(Instant lastRun, LocalTime time, ZoneId zone, Instant now) {
        return lastRun == null || lastRun.isBefore(latestOccurrence(time, zone, now));
    }

    /** Every-N-days job: due at an occurrence at least {@code intervalDays} local calendar days after the last run. */
    public static boolean isIntervalDue(Instant lastRun, int intervalDays, LocalTime time, ZoneId zone, Instant now) {
        if (lastRun == null) return true;
        Instant latest = latestOccurrence(time, zone, now);
        if (!lastRun.isBefore(latest)) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.ofInstant(lastRun, zone), LocalDate.ofInstant(latest, zone));
        return days >= intervalDays;
    }

    /** When an every-N-days job runs next: now-ish if already due, else the first qualifying occurrence. */
    public static Instant nextIntervalRun(Instant lastRun, int intervalDays, LocalTime time, ZoneId zone, Instant now) {
        if (isIntervalDue(lastRun, intervalDays, time, zone, now)) return latestOccurrence(time, zone, now);
        Instant earliest = ZonedDateTime.of(LocalDate.ofInstant(lastRun, zone).plusDays(intervalDays), time, zone).toInstant();
        return earliest.isAfter(now) ? earliest : nextOccurrence(time, zone, now);
    }
}
