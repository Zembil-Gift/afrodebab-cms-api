package com.afrodebab.cms.dto;

public record GitHubAggregateResponse(
        String period, // "YYYY-MM-dd" (Monday start) or "YYYY-MM" (Month start)
        long commits,
        long prsOpened,
        long prsMerged,
        long prsClosed,
        long prReviews,
        long issuesOpened,
        long issuesClosed
) {}
