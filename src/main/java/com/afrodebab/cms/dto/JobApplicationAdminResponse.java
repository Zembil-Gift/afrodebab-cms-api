package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.entity.JobApplicationAnswer;

import java.time.Instant;
import java.util.List;

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
        Instant updatedAt,
        List<JobApplicationAnswer> answers
) {}
