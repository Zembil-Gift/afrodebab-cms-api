package com.afrodebab.cms.dto;

public record TelegramSupportSummaryTotals(
        long pending,
        long inProgress,
        long resolved,
        long total
) {}
