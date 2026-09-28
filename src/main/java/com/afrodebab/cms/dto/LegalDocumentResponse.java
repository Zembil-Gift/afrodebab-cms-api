package com.afrodebab.cms.dto;

import java.time.Instant;

public record LegalDocumentResponse(
        String type,
        String title,
        String content,
        Instant updatedAt
) {}
