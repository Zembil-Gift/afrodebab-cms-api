package com.afrodebab.cms.dto;

import java.util.List;

/** Platform-wide totals plus a per-organization breakdown, for the platform-admin dashboard. */
public record PlatformStatsResponse(
        long totalOrganizations,
        long activeOrganizations,
        long suspendedOrganizations,
        long totalManagers,
        long totalViceManagers,
        long totalSubOrganizations,
        long totalEmployees,
        long totalJobs,
        long totalOpenJobs,
        long totalApplicants,
        long totalHired,
        long totalBlogs,
        long totalEvents,
        List<OrgStatsResponse> organizations
) {}
