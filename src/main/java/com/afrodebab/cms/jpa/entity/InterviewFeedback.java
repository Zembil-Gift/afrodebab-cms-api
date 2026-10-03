package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/** One interviewer's feedback link for an interview, and their feedback once submitted. */
@Data
@EqualsAndHashCode(callSuper = false, exclude = "interview")
@ToString(exclude = "interview")
@Entity
@Table(name = "interview_feedback")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedback extends com.afrodebab.cms.tenant.TenantEntity {
    public enum Recommendation { STRONG_HIRE, HIRE, NO_HIRE, STRONG_NO_HIRE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "interviewer_name")
    private String interviewerName;

    @Column(name = "interviewer_email", nullable = false)
    private String interviewerEmail;

    /** SHA-256 of the link token; null once the feedback is submitted, which kills the link. */
    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private Recommendation recommendation;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "submitted_at")
    private Instant submittedAt;

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
