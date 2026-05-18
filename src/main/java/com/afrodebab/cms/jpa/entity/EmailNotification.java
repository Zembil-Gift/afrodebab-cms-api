package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "email_notifications")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotification {
    public enum DeliveryStatus {
        PENDING,
        SENT,
        FAILED
    }

    public enum NotificationType {
        EMPLOYEE_PASSWORD,
        ADMIN_PAYROLL_REMINDER,
        EMPLOYEE_PAYMENT_RECEIVED,
        HIRING_SELECTED_FOR_INTERVIEW,
        HIRING_REJECTED_PRE_INTERVIEW,
        HIRING_HIRED,
        HIRING_REJECTED_POST_INTERVIEW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

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
