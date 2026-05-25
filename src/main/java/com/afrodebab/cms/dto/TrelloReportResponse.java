package com.afrodebab.cms.dto;

import java.util.List;

public record TrelloReportResponse(
        Long employeeId,
        String employeeName,
        String trelloUsername,
        long cardsCreated,
        long cardsMoved,
        long cardsArchived,
        long commentsAdded,
        long checkItemsCompleted,
        long attachmentsAdded,
        List<TrelloActivityResponse> recentActivities
) {}
