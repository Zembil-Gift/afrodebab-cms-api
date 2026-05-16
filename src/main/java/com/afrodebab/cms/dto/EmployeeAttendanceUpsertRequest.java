package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeAttendanceUpsertRequest(
        @NotNull(message = "date is required") LocalDate date,
        @NotNull(message = "clockInAt is required") Instant clockInAt,
        @NotNull(message = "clockOutAt is required") Instant clockOutAt
) {}
