package com.afrodebab.cms.jpa.entity;

import com.afrodebab.cms.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Per-organization administrator. Performs all in-org management (employees, blogs, jobs,
 * events, attendance, peer reviews, payments) that the former global "Admin" did.
 * Scoped to one Organization via {@link TenantEntity}. Email is globally unique so login
 * needs only email + password (the org is resolved from the record).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "managers")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Manager extends TenantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
