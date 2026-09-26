package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "interviews")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Interview extends com.afrodebab.cms.tenant.TenantEntity {
    public enum Mode { ONLINE, IN_PERSON }
    public enum Status { SCHEDULED, COMPLETED, CANCELLED, NO_SHOW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(name = "scheduled_by_manager_id")
    private Long scheduledByManagerId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode;

    private String location;

    @Column(name = "meeting_url")
    private String meetingUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Event in the scheduling manager's primary Google Calendar; null when invites went out by email. */
    @Column(name = "google_event_id")
    private String googleEventId;

    /** Bumped on every change so calendar apps replace the earlier invitation. */
    @Column(nullable = false)
    private int sequence;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "interview_participants", joinColumns = @JoinColumn(name = "interview_id"))
    @Builder.Default
    private List<InterviewParticipant> participants = new ArrayList<>();

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
