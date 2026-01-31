package com.afrodebab.cms.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApplyRequest(
        @NotBlank(message="fullName is required") String fullName,
        @Email(message="email must be valid") @NotBlank(message="email is required") String email,
        String phoneNumber,
        String githubUrl
) {}

