package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployeeCreateRequest(
        @NotBlank(message = "name is required") String name,
        @Email(message = "email must be valid") @NotBlank(message = "email is required") String email,
        @NotBlank(message = "phone is required") String phone,
        @NotBlank(message = "position is required") String position,
        String linkedinUrl,
        String photo
) {}

