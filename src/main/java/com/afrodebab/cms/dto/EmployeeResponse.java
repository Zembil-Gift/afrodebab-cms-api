package com.afrodebab.cms.dto;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String phone,
        String position,
        String linkedinUrl,
        String photo,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}

