package com.afrodebab.cms.dto;

import java.time.Instant;

/** Per-organization metrics surfaced to platform admins. */
public record OrgStatsResponse(
        Long id,
        String name,
        String slug,
        String status,
        String plan,
        Instant createdAt,
        long managers,
        long viceManagers,
        long subOrganizations,
        long employees,
        long jobs,
        long openJobs,
        long applicants,
        long hired,
        long blogs,
        long publishedBlogs,
        long events,
        long publishedEvents
) {}
