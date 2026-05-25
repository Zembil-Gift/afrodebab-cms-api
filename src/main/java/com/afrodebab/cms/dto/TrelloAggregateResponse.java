package com.afrodebab.cms.dto;

public record TrelloAggregateResponse(
        String period, // "YYYY-MM-dd" (Monday start) or "YYYY-MM" (Month start)
        long cardsCreated,
        long cardsMoved,
        long cardsArchived,
        long commentsAdded,
        long checkItemsCompleted,
        long attachmentsAdded
) {}
