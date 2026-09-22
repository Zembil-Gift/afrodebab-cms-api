package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/** A pending email verification code. Global (not org-scoped): one per purpose and email. */
@Data
@Entity
@Table(name = "email_otps")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtp {
    public enum Purpose {
        SIGNUP,
        MANAGER_PASSWORD_CHANGE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Column(nullable = false) private String email;
    @Column(name = "code_hash", nullable = false) private String codeHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
