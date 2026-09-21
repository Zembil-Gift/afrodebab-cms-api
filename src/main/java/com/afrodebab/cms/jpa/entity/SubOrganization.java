package com.afrodebab.cms.jpa.entity;

import com.afrodebab.cms.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "sub_organizations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sub_org_org_slug", columnNames = {"organization_id", "slug"}),
                @UniqueConstraint(name = "uk_sub_org_org_name", columnNames = {"organization_id", "name"})
        }
)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrganization extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    // --- Geofence Coordinates ---
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "geo_radius_m")
    private Integer geoRadiusM = 200;

    @Column(name = "address_label")
    private String addressLabel;

    // --- Sub-Org Attendance Overrides ---
    @Column(name = "entry_time")
    private LocalTime entryTime;

    @Column(name = "exit_time")
    private LocalTime exitTime;

    @Column(name = "lunch_start_time")
    private LocalTime lunchStartTime;

    @Column(name = "lunch_end_time")
    private LocalTime lunchEndTime;

    @Column(name = "grace_minutes")
    private Integer graceMinutes;

    @Column(name = "max_lunch_break_minutes")
    private Integer maxLunchBreakMinutes;

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
