package com.afrodebab.cms.service;

import com.afrodebab.cms.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

/**
 * Enforces that a login email belongs to at most one account across platform admins,
 * managers (incl. vice managers) and employees of every organization, and that an
 * organization's company email isn't reused by another organization.
 *
 * Native SQL on purpose: Hibernate's {@code @TenantId} filter is fixed when the session opens,
 * so repository lookups inside a manager's transaction only see that manager's organization.
 */
@Service
public class EmailUniquenessService {

    public enum AccountType { PLATFORM_ADMIN, MANAGER, EMPLOYEE }

    private static final String ACCOUNT_OWNERS_SQL = """
            SELECT COUNT(*) FROM (
                SELECT 'PLATFORM_ADMIN' AS type, id FROM platform_admins WHERE LOWER(email) = ?
                UNION ALL SELECT 'MANAGER', id FROM managers WHERE LOWER(email) = ?
                UNION ALL SELECT 'EMPLOYEE', id FROM employees WHERE LOWER(email) = ?
            ) owners
            WHERE NOT (owners.type = ? AND owners.id = ?)
            """;

    private final JdbcTemplate jdbc;

    public EmailUniquenessService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** For a brand-new account. */
    @Transactional(readOnly = true)
    public void assertAccountEmailAvailable(String email) {
        assertAccountEmailAvailable(email, null, null);
    }

    /** For an existing account changing its email; that account itself doesn't count as a clash. */
    @Transactional(readOnly = true)
    public void assertAccountEmailAvailable(String email, AccountType selfType, Long selfId) {
        String normalized = normalize(email);
        Long owners = jdbc.queryForObject(ACCOUNT_OWNERS_SQL, Long.class, normalized, normalized, normalized,
                selfType == null ? "" : selfType.name(), Objects.requireNonNullElse(selfId, -1L));
        if (owners != null && owners > 0) {
            throw new BadRequestException("An account with this email already exists");
        }
    }

    /** selfOrgId null = a new organization. */
    @Transactional(readOnly = true)
    public void assertCompanyEmailAvailable(String email, Long selfOrgId) {
        if (email == null || email.isBlank()) return;
        Long owners = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE LOWER(company_email) = ? AND id <> ?",
                Long.class, normalize(email), Objects.requireNonNullElse(selfOrgId, -1L));
        if (owners != null && owners > 0) {
            throw new BadRequestException("Another organization already uses this company email");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
