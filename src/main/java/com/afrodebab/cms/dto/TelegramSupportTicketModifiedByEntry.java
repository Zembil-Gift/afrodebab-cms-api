package com.afrodebab.cms.dto;

public record TelegramSupportTicketModifiedByEntry(
        String status,
        String changedAt,
        String adminUsername
) {}
