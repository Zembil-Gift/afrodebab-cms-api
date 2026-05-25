package com.afrodebab.cms.dto;

import java.util.List;

public record TelegramSupportReportResponse(
        Long employeeId,
        String employeeName,
        String telegramUsername,
        TelegramSupportSummaryTotals totals,
        TelegramSupportSummaryAverages averages,
        List<TelegramSupportSummaryCountByIssueType> countsByIssueType,
        List<TelegramSupportTicketResponse> recentTickets
) {}
