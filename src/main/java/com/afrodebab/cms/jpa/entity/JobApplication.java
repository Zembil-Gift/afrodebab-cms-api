package com.afrodebab.cms.jpa.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@Entity
@Table(name = "job_applications")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name="full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(name="phone_number")
    private String phoneNumber;

    @Column(name="github_url")
    private String githubUrl;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); }

}

