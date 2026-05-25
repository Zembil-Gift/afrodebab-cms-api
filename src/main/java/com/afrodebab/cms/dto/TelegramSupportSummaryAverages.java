package com.afrodebab.cms.dto;

public record TelegramSupportSummaryAverages(
        Long msFromFirstStatusChangeToResolved,
        Long msFromCreatedAtToResolved
) {}
