package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailPreviewRequest(
        @Email(message = "email must be valid") @NotBlank(message = "email is required") String email,
        @NotBlank(message = "emailCase is required") String emailCase
) {}

