package com.afrodebab.cms.dto;

import java.util.List;

public record TelegramSupportTicketsResponse(
        TelegramSupportTicketsFilters filters,
        TelegramSupportTicketsPagination pagination,
        List<TelegramSupportTicketResponse> tickets
) {}
