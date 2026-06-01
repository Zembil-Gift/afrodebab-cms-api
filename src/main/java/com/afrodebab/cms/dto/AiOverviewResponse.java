package com.afrodebab.cms.dto;

import java.time.Instant;

public record AiOverviewResponse(
        Long applicationId,
        String fullName,
        String jobTitle,
        String aiOverviewText,
        String aiOverviewStatus,
        String aiOverviewError,
        Integer aiOverviewAttemptCount,
        Instant aiOverviewCompletedAt
) {}
