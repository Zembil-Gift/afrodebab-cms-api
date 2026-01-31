package com.afrodebab.cms.dto;

import java.time.Instant;

public record JobApplicationAdminResponse(
        Long id,
        Long jobId,
        String fullName,
        String email,
        String phoneNumber,
        String githubUrl,
        Instant createdAt
) {}

