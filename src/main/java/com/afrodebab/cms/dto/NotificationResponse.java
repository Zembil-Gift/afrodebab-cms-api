package com.afrodebab.cms.dto;

import java.time.Instant;

/** {@code bodyHtml} is pre-sanitized by SimpleMarkdown and safe to render as HTML. */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String bodyHtml,
        String link,
        boolean read,
        Instant createdAt
) {}
