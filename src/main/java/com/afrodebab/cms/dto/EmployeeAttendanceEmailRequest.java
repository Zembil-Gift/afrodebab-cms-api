package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployeeAttendanceEmailRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email
) {}
