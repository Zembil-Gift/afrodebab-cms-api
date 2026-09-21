package com.afrodebab.cms.dto;

import java.time.Instant;

public record ViceManagerResponse(
        Long id,
        String name,
        String email,
        boolean active,
        Long subOrganizationId,
        String subOrganizationName,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {}
