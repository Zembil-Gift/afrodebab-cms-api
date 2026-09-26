package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** A message a manager or vice manager sent to the whole org or chosen branches. */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "broadcasts")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Broadcast extends com.afrodebab.cms.tenant.TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_manager_id")
    private Long senderManagerId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String subject;

    /** Markdown subset, rendered by {@link com.afrodebab.cms.util.SimpleMarkdown}. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "send_email", nullable = false)
    private boolean sendEmail;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    /** Targeted branches; empty = everyone. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "broadcast_sub_organizations", joinColumns = @JoinColumn(name = "broadcast_id"))
    @Column(name = "sub_organization_id", nullable = false)
    @Builder.Default
    private Set<Long> subOrganizationIds = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
