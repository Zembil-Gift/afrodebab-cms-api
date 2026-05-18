package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String phone,
        String position,
        String linkedinUrl,
        String photo,
        boolean active,
        LocalDate salaryDate,
        Long salaryAmountMinor,
        List<DayOfWeek> salaryScheduleDays,
        Instant createdAt,
        Instant updatedAt
) {}
