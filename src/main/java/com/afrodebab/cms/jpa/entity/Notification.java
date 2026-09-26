package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/** In-app notification for exactly one recipient: an employee or a manager/vice manager. */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notifications")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends com.afrodebab.cms.tenant.TenantEntity {
    public enum Type { BROADCAST, INTERVIEW_INVITATION, INTERVIEW_CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "manager_id")
    private Long managerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String title;

    /** Markdown subset, rendered by {@link com.afrodebab.cms.util.SimpleMarkdown}. */
    @Column(columnDefinition = "TEXT")
    private String body;

    private String link;

    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
