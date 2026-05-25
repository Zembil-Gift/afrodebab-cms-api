package com.afrodebab.cms.dto;

public record TelegramSupportSummaryCountByAdmin(
        String adminUsername,
        long count
) {}
