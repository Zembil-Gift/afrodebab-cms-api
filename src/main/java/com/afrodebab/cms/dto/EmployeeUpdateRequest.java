package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;

public record EmployeeUpdateRequest(
        String name,
        @Email(message = "email must be valid") String email,
        String phone,
        String position,
        String linkedinUrl,
        String photo,
        Boolean active
) {}

