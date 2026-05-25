package com.afrodebab.cms.dto;

public record TelegramSupportTicketsPagination(
        long page,
        long pageSize,
        long total,
        long totalPages
) {}
