package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "employees")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String position;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "trello_username")
    private String trelloUsername;

    @Column(name = "telegram_username")
    private String telegramUsername;

    private String photo;

    private String role;
    private String department;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "employee_status")
    private String employeeStatus;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "salary_effective_date")
    private LocalDate salaryEffectiveDate;

    @Column(name = "salary_amount_minor")
    private Long salaryAmountMinor;

    @ElementCollection
    @CollectionTable(
            name = "employee_office_schedule_days",
            joinColumns = @JoinColumn(name = "employee_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_employee_office_schedule_days_employee_day",
                    columnNames = {"employee_id", "schedule_day"}
            )
    )
    @Column(name = "schedule_day", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> officeDays = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
