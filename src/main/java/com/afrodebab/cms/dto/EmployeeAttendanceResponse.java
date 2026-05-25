package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record EmployeeAttendanceResponse(
        Long id,
        Long employeeId,
        LocalDate date,
        Instant clockInAt,
        Instant clockOutAt,
        Instant lunchBreakInAt,
        Instant lunchBreakOutAt,
        Map<String, String> attendanceStatus,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
