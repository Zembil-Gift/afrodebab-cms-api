package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeCreateRequest(
        @NotBlank(message = "name is required") String name,
        @Email(message = "email must be valid") @NotBlank(message = "email is required") String email,
        @NotBlank(message = "phone is required") String phone,
        @NotBlank(message = "position is required") String position,
        String role,
        String department,
        String employmentType,
        String employeeStatus,
        String linkedinUrl,
        String photo,
        String githubUsername,
        String trelloUsername,
        String telegramUsername,
        LocalDate salaryDate,
        @PositiveOrZero(message = "salaryAmountMinor must be zero or positive") Long salaryAmountMinor,
        @NotNull(message = "salaryScheduleDays is required") @NotEmpty(message = "salaryScheduleDays must not be empty") Set<DayOfWeek> salaryScheduleDays
) {}
