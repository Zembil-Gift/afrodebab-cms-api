package com.afrodebab.cms.dto;

import java.time.Instant;
import java.util.List;

public record BroadcastResponse(
        Long id,
        String subject,
        String body,
        String bodyHtml,
        String senderName,
        List<SubOrganizationRef> subOrganizations,
        boolean sendEmail,
        int recipientCount,
        long readCount,
        Instant createdAt
) {
    public record SubOrganizationRef(Long id, String name) {}
}
