package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "support_metrics")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "telegram_ticket_id")
    private String telegramTicketId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "issue_type")
    private String issueType;

    @Column(name = "status")
    private String status;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
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
