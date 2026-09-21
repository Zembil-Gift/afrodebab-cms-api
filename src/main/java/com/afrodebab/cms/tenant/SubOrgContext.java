package com.afrodebab.cms.tenant;

import java.util.function.Supplier;

/**
 * Holds the current request's sub-organization id in a ThreadLocal so Vice Manager
 * operations can be automatically scoped to their designated branch/sub-organization.
 * Populated by JwtAuthFilter (from the token's {@code subOrgId} claim) and cleared
 * at the end of each request.
 */
public final class SubOrgContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private SubOrgContext() {}

    public static void set(Long subOrgId) {
        CURRENT.set(subOrgId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T callAs(Long subOrgId, Supplier<T> work) {
        Long previous = CURRENT.get();
        CURRENT.set(subOrgId);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
