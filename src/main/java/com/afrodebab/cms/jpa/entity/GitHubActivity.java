package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "github_activities")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "activity_type", nullable = false)
    private String activityType; // 'COMMIT', 'PR_OPENED', 'PR_MERGED', 'PR_CLOSED', 'PR_REVIEW', 'ISSUE_OPENED', 'ISSUE_CLOSED'

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "activity_id", nullable = false, unique = true)
    private String activityId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "url")
    private String url;

    @Column(name = "activity_timestamp", nullable = false)
    private Instant activityTimestamp;

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
