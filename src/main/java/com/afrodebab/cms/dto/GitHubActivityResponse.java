package com.afrodebab.cms.dto;

import java.time.Instant;

public record GitHubActivityResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String githubUsername,
        String activityType,
        String repository,
        String activityId,
        String title,
        String description,
        String url,
        Instant activityTimestamp,
        Instant createdAt,
        Instant updatedAt
) {}
