package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeUpdateRequest(
        String name,
        @Email(message = "email must be valid") String email,
        String phone,
        String position,
        String linkedinUrl,
        String photo,
        Boolean active,
        LocalDate salaryDate,
        @PositiveOrZero(message = "salaryAmountMinor must be zero or positive") Long salaryAmountMinor,
        Set<DayOfWeek> salaryScheduleDays
) {}
