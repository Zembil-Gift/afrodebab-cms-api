package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeCreateRequest(
        @NotBlank(message = "name is required") String name,
        @Email(message = "email must be valid") @NotBlank(message = "email is required") String email,
        @NotBlank(message = "phone is required") String phone,
        @NotBlank(message = "position is required") String position,
        String linkedinUrl,
        String photo,
        LocalDate salaryDate,
        @PositiveOrZero(message = "salaryAmountMinor must be zero or positive") Long salaryAmountMinor,
        Set<DayOfWeek> salaryScheduleDays
) {}
