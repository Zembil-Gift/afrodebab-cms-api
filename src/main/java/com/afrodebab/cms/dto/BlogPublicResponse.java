package com.afrodebab.cms.dto;

import java.time.Instant;

public record BlogPublicResponse(
        Long id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImageUrl,
        Instant publishedAt
) {}

