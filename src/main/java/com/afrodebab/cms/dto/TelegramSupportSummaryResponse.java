package com.afrodebab.cms.dto;

import java.util.List;

public record TelegramSupportSummaryResponse(
        TelegramSupportSummaryFilters filters,
        TelegramSupportSummaryTotals totals,
        List<TelegramSupportSummaryCountByIssueType> countsByIssueType,
        List<TelegramSupportSummaryCountByAdmin> countsByAdminUsername,
        TelegramSupportSummaryAverages averages
) {}
