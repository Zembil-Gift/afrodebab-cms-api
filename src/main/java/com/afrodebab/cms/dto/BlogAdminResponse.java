package com.afrodebab.cms.dto;

import java.time.Instant;
import com.afrodebab.cms.jpa.entity.Blog;

public record BlogAdminResponse(
        Long id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImageUrl,
        Blog.Status status,
        Instant publishedAt
) {}

