package com.afrodebab.cms.dto;

import java.util.List;

public record GitHubReportResponse(
        Long employeeId,
        String employeeName,
        String githubUsername,
        long totalCommits,
        long prsOpened,
        long prsMerged,
        long prsClosed,
        long prReviews,
        long issuesOpened,
        long issuesClosed,
        List<GitHubActivityResponse> recentActivities
) {}
