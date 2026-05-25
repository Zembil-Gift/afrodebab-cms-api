package com.afrodebab.cms.dto;

public record TelegramSupportSummaryCountByIssueType(
        String issueType,
        long count
) {}
