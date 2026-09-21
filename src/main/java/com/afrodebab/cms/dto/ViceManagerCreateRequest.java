package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ViceManagerCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,

        @NotNull(message = "subOrganizationId is required")
        Long subOrganizationId
) {}
