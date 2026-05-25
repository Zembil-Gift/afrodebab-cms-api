package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeAttendanceUpsertRequest(
        @NotNull(message = "date is required") LocalDate date,
        Instant clockInAt,
        Instant clockOutAt,
        Instant lunchBreakInAt,
        Instant lunchBreakOutAt,
        String notes
) {}
