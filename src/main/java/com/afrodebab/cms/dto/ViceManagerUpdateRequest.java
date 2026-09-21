package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Size;

public record ViceManagerUpdateRequest(
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        Long subOrganizationId,

        Boolean active
) {}
