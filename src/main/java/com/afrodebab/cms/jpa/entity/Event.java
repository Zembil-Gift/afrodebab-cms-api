package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "events")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    public enum Status { DRAFT, PUBLISHED }
    public enum EventType { ONLINE, IN_PERSON }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String title;
    @Column(nullable = false, unique = true) private String slug;

    @Lob @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name="event_type", nullable = false)
    private EventType eventType;

    private String location;

    @Column(name="start_date", nullable = false)
    private Instant startDate;

    @Column(name="end_date")
    private Instant endDate;

    @Column(name="registration_url")
    private String registrationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}

