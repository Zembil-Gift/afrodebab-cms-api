package com.afrodebab.cms.dto;

import java.time.Instant;

public record TrelloActivityResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String trelloUsername,
        String activityType,
        String boardName,
        String cardId,
        String cardName,
        String listName,
        String activityId,
        String description,
        String url,
        Instant activityTimestamp,
        Instant createdAt,
        Instant updatedAt
) {}
