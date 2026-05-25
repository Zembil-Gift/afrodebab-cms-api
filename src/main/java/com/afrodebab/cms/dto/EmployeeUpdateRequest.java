package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeUpdateRequest(
        String name,
        @Email(message = "email must be valid") String email,
        String phone,
        String position,
        String role,
        String department,
        String employmentType,
        String employeeStatus,
        String linkedinUrl,
        String photo,
        String githubUsername,
        String trelloUsername,
        String telegramUsername,
        Boolean active,
        LocalDate salaryDate,
        @PositiveOrZero(message = "salaryAmountMinor must be zero or positive") Long salaryAmountMinor,
        @NotEmpty(message = "salaryScheduleDays must not be empty") Set<DayOfWeek> salaryScheduleDays
) {}
