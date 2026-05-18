package com.afrodebab.cms.jpa.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "job_applications")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {
    public enum ApplicationStatus {
        APPLIED,
        UNDER_REVIEW,
        SELECTED_FOR_INTERVIEW,
        REJECTED,
        HIRED
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name="full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(name="phone_number")
    private String phoneNumber;

    @Column(name="github_url")
    private String githubUrl;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hired_employee_id")
    private Employee hiredEmployee;

    @Column(name="created_at", nullable=true, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=true)
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
