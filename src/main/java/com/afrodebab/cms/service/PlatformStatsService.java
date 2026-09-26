package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.OrgStatsResponse;
import com.afrodebab.cms.dto.PlatformStatsResponse;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes cross-organization metrics for platform admins. Counts are gathered with native
 * {@code GROUP BY organization_id} queries, which bypass Hibernate's {@code @TenantId} filter,
 * so a single query yields every org's total for a table. {@link Organization} itself is not
 * tenant-scoped, so listing all orgs works regardless of the (absent) tenant on an admin request.
 */
@Service
public class PlatformStatsService {

    private final OrganizationRepository orgRepo;
    private final EntityManager em;

    public PlatformStatsService(OrganizationRepository orgRepo, EntityManager em) {
        this.orgRepo = orgRepo;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public PlatformStatsResponse overview() {
        List<Organization> orgs = orgRepo.findAll();

        Map<Long, Long> managers = countByOrg("managers", null);
        Map<Long, Long> employees = countByOrg("employees", null);
        Map<Long, Long> jobs = countByOrg("jobs", null);
        Map<Long, Long> openJobs = countByOrg("jobs", "status = 'OPEN'");
        Map<Long, Long> applicants = countByOrg("job_applications", null);
        Map<Long, Long> hired = countByOrg("job_applications", "status = 'HIRED'");
        Map<Long, Long> blogs = countByOrg("blogs", null);
        Map<Long, Long> publishedBlogs = countByOrg("blogs", "status = 'PUBLISHED'");
        Map<Long, Long> events = countByOrg("events", null);
        Map<Long, Long> publishedEvents = countByOrg("events", "status = 'PUBLISHED'");

        List<OrgStatsResponse> perOrg = orgs.stream().map(o -> new OrgStatsResponse(
                o.getId(), o.getName(), o.getSlug(), o.getStatus(), o.getPlan(), o.getCreatedAt(),
                managers.getOrDefault(o.getId(), 0L),
                employees.getOrDefault(o.getId(), 0L),
                jobs.getOrDefault(o.getId(), 0L),
                openJobs.getOrDefault(o.getId(), 0L),
                applicants.getOrDefault(o.getId(), 0L),
                hired.getOrDefault(o.getId(), 0L),
                blogs.getOrDefault(o.getId(), 0L),
                publishedBlogs.getOrDefault(o.getId(), 0L),
                events.getOrDefault(o.getId(), 0L),
                publishedEvents.getOrDefault(o.getId(), 0L)
        )).toList();

        long active = orgs.stream().filter(o -> "ACTIVE".equalsIgnoreCase(o.getStatus())).count();
        long suspended = orgs.stream().filter(o -> "SUSPENDED".equalsIgnoreCase(o.getStatus())).count();

        return new PlatformStatsResponse(
                orgs.size(), active, suspended,
                perOrg.stream().mapToLong(OrgStatsResponse::managers).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::employees).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::jobs).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::openJobs).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::applicants).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::hired).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::blogs).sum(),
                perOrg.stream().mapToLong(OrgStatsResponse::events).sum(),
                perOrg
        );
    }

    /**
     * Returns organization_id -> row count for {@code table}, optionally filtered by a fixed
     * {@code whereClause}. Both arguments are hard-coded constants (never request input), so the
     * concatenated SQL carries no injection risk.
     */
    private Map<Long, Long> countByOrg(String table, String whereClause) {
        String sql = "SELECT organization_id, COUNT(*) FROM " + table
                + (whereClause == null ? "" : " WHERE " + whereClause)
                + " GROUP BY organization_id";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        Map<Long, Long> byOrg = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            byOrg.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return byOrg;
    }
}
