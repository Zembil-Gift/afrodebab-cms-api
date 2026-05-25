package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "task_metrics")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "source")
    private String source;

    @Column(name = "source_task_id")
    private String sourceTaskId;

    @Column(name = "task_title")
    private String taskTitle;

    @Column(name = "status")
    private String status;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_overdue")
    private Boolean overdue;

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
