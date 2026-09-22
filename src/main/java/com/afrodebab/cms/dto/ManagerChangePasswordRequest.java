package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ManagerChangePasswordRequest(
        @NotBlank(message = "verification code is required")
        @Pattern(regexp = "\\d{6}", message = "verification code must be 6 digits")
        String otp,
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 72, message = "newPassword must be 8 to 72 characters")
        String newPassword
) {}
