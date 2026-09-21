package com.afrodebab.cms.dto;

import java.time.Instant;

/**
 * dispatchTime is local to timezone; dispatchTimeUtc is the same moment in UTC for today
 * (it can shift by an hour across DST changes). Instants are UTC.
 */
public record EmailScheduleResponse(
        String dispatchTime,
        String timezone,
        int payrollReminderIntervalDays,
        String dispatchTimeUtc,
        Instant nextDispatchAt,
        Instant nextPayrollReminderAt,
        Instant lastEmailDispatchAt,
        Instant lastPayrollReminderAt
) {}
