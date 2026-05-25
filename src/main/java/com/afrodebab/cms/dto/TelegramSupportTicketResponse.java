package com.afrodebab.cms.dto;

import java.util.List;

public record TelegramSupportTicketResponse(
        Long id,
        String ticketCode,
        String customerTelegramChatId,
        Long customerId,
        String customerEmail,
        String customerName,
        String customerTelegramUsername,
        String orderId,
        String issueType,
        String description,
        String status,
        List<TelegramSupportTicketModifiedByEntry> modifiedBy,
        String createdAt,
        String updatedAt
) {}
