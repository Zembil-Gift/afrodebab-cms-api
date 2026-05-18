package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.JobApplication;
import jakarta.validation.constraints.NotNull;

public record JobApplicationStatusUpdateRequest(
        @NotNull(message = "status is required")
        JobApplication.ApplicationStatus status
) {}
