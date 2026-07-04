package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Organization;

import java.time.Instant;

public record OrgResponse(
        Long id,
        String name,
        String slug,
        String status,
        String plan,
        Instant createdAt
) {
    public static OrgResponse from(Organization o) {
        return new OrgResponse(o.getId(), o.getName(), o.getSlug(), o.getStatus(), o.getPlan(), o.getCreatedAt());
    }
}
