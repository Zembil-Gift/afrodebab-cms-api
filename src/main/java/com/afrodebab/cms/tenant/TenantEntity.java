package com.afrodebab.cms.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.TenantId;

/**
 * Base class for every organization-owned entity. The {@link TenantId} column is
 * populated automatically by Hibernate on insert (from {@link TenantIdentifierResolver})
 * and every select/update is automatically constrained to the current tenant, so no
 * repository or service query can leak data across organizations.
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class TenantEntity {

    @TenantId
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;
}
