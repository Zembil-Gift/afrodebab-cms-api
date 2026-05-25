package com.afrodebab.cms.dto;

public record TelegramSupportSummaryFilters(
        String adminUsername,
        String typeGroup,
        String from,
        String to
) {}
