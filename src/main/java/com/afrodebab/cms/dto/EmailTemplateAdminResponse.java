package com.afrodebab.cms.dto;

import java.util.List;

/** One notification type in the email builder: the org's override (null = default) next to the defaults. */
public record EmailTemplateAdminResponse(
        String type,
        String audience,
        String subject,
        String heading,
        String message,
        boolean customized,
        String defaultSubject,
        String defaultHeading,
        String defaultMessage,
        List<String> placeholders
) {}
