package com.afrodebab.cms.dto;

public record TelegramSupportTicketsFilters(
        String adminUsername,
        String typeGroup,
        String status,
        String from,
        String to
) {}
