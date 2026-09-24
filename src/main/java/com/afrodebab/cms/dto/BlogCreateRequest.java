package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Blog;
import jakarta.validation.constraints.NotBlank;

public record BlogCreateRequest(
        @NotBlank(message = "title is required") String title,
        String slug,              // optional: if null, we generate
        String excerpt,
        @NotBlank(message = "content is required") String content,
        String coverImageUrl,
        Blog.Status status        // optional: defaults to DRAFT
) {}

