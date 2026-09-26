package com.afrodebab.cms.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Feeds the current organization id from {@link TenantContext} into Hibernate's
 * discriminator-based multi-tenancy. Registered with Hibernate via the
 * {@link HibernatePropertiesCustomizer} so every session resolves its tenant from the
 * active request.
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.getOrNoTenant();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /**
     * When the current tenant is {@link TenantContext#ROOT}, Hibernate skips the tenant
     * discriminator so pre-auth login lookups can find a user across all organizations.
     */
    @Override
    public boolean isRoot(Long tenantId) {
        return TenantContext.ROOT.equals(tenantId);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
