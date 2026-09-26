package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/** The spreadsheet a manager keeps in sync, and which datasets it holds (one tab each). */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "google_sheet_syncs")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleSheetSync extends com.afrodebab.cms.tenant.TenantEntity {
    public enum Dataset { EMPLOYEES, ATTENDANCE, PAYMENTS, METRICS, JOB_APPLICATIONS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manager_id", nullable = false, unique = true)
    private Long managerId;

    @Column(name = "spreadsheet_id", nullable = false)
    private String spreadsheetId;

    @Column(name = "spreadsheet_url", nullable = false)
    private String spreadsheetUrl;

    /** Comma-separated {@link Dataset} names. */
    @Column(nullable = false)
    private String datasets;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
