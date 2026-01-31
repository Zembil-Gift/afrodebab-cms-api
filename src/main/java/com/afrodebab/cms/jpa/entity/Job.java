package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "jobs")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
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

    @Lob @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}

