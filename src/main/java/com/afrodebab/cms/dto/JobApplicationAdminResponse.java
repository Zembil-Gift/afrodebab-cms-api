package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.JobApplication;

import java.time.Instant;

public record JobApplicationAdminResponse(
        Long id,
        Long jobId,
        String fullName,
        String email,
        String phoneNumber,
        String githubUrl,
        String resumeUrl,
        JobApplication.ApplicationStatus status,
        Long hiredEmployeeId,
        Instant createdAt,
        Instant updatedAt
) {}
