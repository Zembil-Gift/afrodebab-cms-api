package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Blog;

public record BlogUpdateRequest(
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImageUrl,
        Blog.Status status
) {}
