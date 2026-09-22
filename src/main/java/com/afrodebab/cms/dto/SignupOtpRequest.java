package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupOtpRequest(
        @Email(message = "a valid email is required") @NotBlank(message = "email is required") @Size(max = 200) String email
) {}
