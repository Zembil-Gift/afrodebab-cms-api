package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record HireCandidateRequest(
        @NotNull(message = "applicationId is required")
        Long applicationId,
        @NotBlank(message = "phone is required")
        String phone,
        @NotBlank(message = "position is required")
        String position,
        @NotNull(message = "salaryDate is required")
        LocalDate salaryDate,
        @NotNull(message = "salaryAmountMinor is required")
        @Positive(message = "salaryAmountMinor must be greater than zero")
        Long salaryAmountMinor
) {}
