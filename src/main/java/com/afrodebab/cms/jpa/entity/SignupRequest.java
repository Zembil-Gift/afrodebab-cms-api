package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * A self-serve "Start free" request from the public site. Global (not org-scoped): it exists
 * before any organization does. A platform admin reviews it and, on approval, provisions the
 * organization and its first manager, then this row is marked {@code APPROVED}.
 */
@Data
@Entity
@Table(name = "signup_requests")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false) private String companyName;
    @Column(name = "contact_name", nullable = false) private String contactName;
    @Column(nullable = false) private String email;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
