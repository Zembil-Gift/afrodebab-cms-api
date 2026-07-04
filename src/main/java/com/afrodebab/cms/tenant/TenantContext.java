package com.afrodebab.cms.tenant;

/**
 * Holds the current request's organization id in a ThreadLocal so Hibernate's
 * {@code @TenantId} filtering can scope every query without callers passing the org
 * around explicitly. Populated by JwtAuthFilter (from the token's {@code orgId} claim)
 * and by the public-endpoint org resolver (from the URL slug). Always cleared at the
 * end of the request.
 */
public final class TenantContext {

    /** Sentinel used when no organization is in scope (e.g. platform-admin requests). */
    public static final Long NO_TENANT = 0L;

    /**
     * Sentinel that disables tenant filtering entirely so a query can read across all
     * organizations. Only for pre-authentication lookups (resolving a user's org from
     * their globally-unique email at login). Never set this from a normal request.
     */
    public static final Long ROOT = -1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long organizationId) {
        CURRENT.set(organizationId);
    }

    /**
     * Runs {@code work} with tenant filtering disabled (root scope), restoring the prior
     * tenant afterwards. Used at login to resolve a user by email across all orgs.
     */
    public static <T> T callAsRoot(java.util.function.Supplier<T> work) {
        return callAs(ROOT, work);
    }

    /**
     * Runs {@code work} scoped to {@code organizationId}, restoring the prior tenant
     * afterwards. Used e.g. to create an org's first Manager under that org's scope.
     */
    public static <T> T callAs(Long organizationId, java.util.function.Supplier<T> work) {
        Long previous = CURRENT.get();
        CURRENT.set(organizationId);
        try {
            return work.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    /** Current organization id, or {@link #NO_TENANT} if none is set. */
    public static Long getOrNoTenant() {
        Long id = CURRENT.get();
        return id == null ? NO_TENANT : id;
    }

    /** Current organization id, or null if none is set. */
    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
