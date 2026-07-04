package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Top-level tenant. Every Manager, Employee and all business data belongs to exactly one
 * Organization. This table is global (not tenant-scoped) and is managed by PlatformAdmins.
 */
@Data
@Entity
@Table(name = "organizations")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** URL-safe identifier used by public endpoints (/public/{slug}/...). */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "plan", nullable = false)
    private String plan = "FREE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
