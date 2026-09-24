package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "jobs")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Job extends com.afrodebab.cms.tenant.TenantEntity {
    public enum Status { DRAFT, OPEN, CLOSED }
    public enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT, INTERN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String title;
    @Column(nullable = false, unique = true) private String slug;

    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name="employment_type", nullable = false)
    private EmploymentType employmentType;

    private String location;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name = "experience_level", length = 40)
    private String experienceLevel;

    @Column(name = "salary_range", length = 120)
    private String salaryRange;

    /** Last day (inclusive) applications are accepted; null = no deadline. */
    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "application_fields", nullable = false)
    private List<JobApplicationField> applicationFields = new ArrayList<>();

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}

