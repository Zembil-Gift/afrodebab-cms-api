package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "employee_metric_scores")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeMetricScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "leadership_score")
    private BigDecimal leadershipScore;

    @Column(name = "attendance_score")
    private BigDecimal attendanceScore;

    @Column(name = "task_score")
    private BigDecimal taskScore;

    @Column(name = "support_score")
    private BigDecimal supportScore;

    @Column(name = "overall_score")
    private BigDecimal overallScore;

    @Column(name = "strength_summary")
    private String strengthSummary;

    @Column(name = "improvement_summary")
    private String improvementSummary;

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
