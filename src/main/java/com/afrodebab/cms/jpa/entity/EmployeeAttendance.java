package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Entity
@Table(
        name = "employee_attendance",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_attendance_employee_date", columnNames = {"employee_id", "attendance_date"})
        }
)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendance {
    public enum AttendanceFinalStatus {
        ON_TIME,
        LATE,
        ABSENT,
        APPROVED_LEAVE,
        REMOTE_APPROVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "clock_in_at")
    private Instant clockInAt;

    @Column(name = "clock_out_at")
    private Instant clockOutAt;

    @Column(name = "lunch_break_in_at")
    private Instant lunchBreakInAt;

    @Column(name = "lunch_break_out_at")
    private Instant lunchBreakOutAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attendance_status")
    private Map<String, String> attendanceStatus = new LinkedHashMap<>();

    @Column(name = "notes")
    private String notes;

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
