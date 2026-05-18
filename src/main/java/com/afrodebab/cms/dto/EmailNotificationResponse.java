package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.EmailNotification;

import java.time.Instant;

public record EmailNotificationResponse(
        Long id,
        EmailNotification.NotificationType type,
        EmailNotification.DeliveryStatus status,
        String recipientEmail,
        String subject,
        int attemptCount,
        String lastError,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt
) {}
