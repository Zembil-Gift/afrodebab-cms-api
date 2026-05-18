package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeAttendanceResponse(
        Long id,
        Long employeeId,
        LocalDate date,
        Instant clockInAt,
        Instant clockOutAt,
        Instant lunchBreakInAt,
        Instant lunchBreakOutAt,
        Instant createdAt,
        Instant updatedAt
) {}
