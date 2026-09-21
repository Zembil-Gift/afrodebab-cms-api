package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Size;

/** A template override (also used as an unsaved draft for preview/test). Blank fields mean "use the default". */
public record EmailTemplateUpdateRequest(
        @Size(max = 255, message = "subject must be at most 255 characters") String subject,
        @Size(max = 255, message = "heading must be at most 255 characters") String heading,
        @Size(max = 5000, message = "message must be at most 5000 characters") String message
) {}
