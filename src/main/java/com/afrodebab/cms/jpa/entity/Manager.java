package com.afrodebab.cms.jpa.entity;

import com.afrodebab.cms.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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

    public enum ManagerRole {
        MANAGER,
        VICE_MANAGER
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private ManagerRole role = ManagerRole.MANAGER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_organization_id")
    private SubOrganization subOrganization;

    /** This manager's personal Trello token, encrypted at rest. Null until they connect. */
    @Column(name = "trello_token", columnDefinition = "TEXT")
    private String trelloToken;

    /** Trello boards this manager chose to track. May span multiple boards in one org. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "manager_trello_boards", joinColumns = @JoinColumn(name = "manager_id"))
    @Builder.Default
    private Set<TrelloBoardRef> trelloBoards = new HashSet<>();

    /** This manager's personal GitHub OAuth token, encrypted at rest. Null until they connect. */
    @Column(name = "github_token", columnDefinition = "TEXT")
    private String githubToken;

    /** GitHub organizations this manager chose to track. May span multiple orgs. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "manager_github_orgs", joinColumns = @JoinColumn(name = "manager_id"))
    @Builder.Default
    private Set<GitHubOrgRef> githubOrgs = new HashSet<>();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
