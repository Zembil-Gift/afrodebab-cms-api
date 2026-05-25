package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "trello_activities")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TrelloActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "trello_username", nullable = false)
    private String trelloUsername;

    @Column(name = "activity_type", nullable = false)
    private String activityType; // 'CARD_CREATED', 'CARD_MOVED', 'CARD_ARCHIVED', 'COMMENT_ADDED', 'CHECKITEM_COMPLETED', 'ATTACHMENT_ADDED'

    @Column(name = "board_name", nullable = false)
    private String boardName;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(name = "card_name")
    private String cardName;

    @Column(name = "list_name")
    private String listName;

    @Column(name = "activity_id", nullable = false, unique = true)
    private String activityId;

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
