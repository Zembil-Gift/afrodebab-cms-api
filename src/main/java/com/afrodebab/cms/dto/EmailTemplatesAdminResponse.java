package com.afrodebab.cms.dto;

import java.util.List;

/** The email builder: branding plus every customizable notification type. */
public record EmailTemplatesAdminResponse(
        String emailLogoUrl,
        String effectiveLogoUrl,
        List<EmailTemplateAdminResponse> templates
) {}
