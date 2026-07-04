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
@Table(
        name = "employee_payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_payments_employee_cycle_start", columnNames = {"employee_id", "cycle_start_date"})
        }
)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayment extends com.afrodebab.cms.tenant.TenantEntity {
    public enum PaymentStatus {
        PENDING,
        PAID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Net (take-home) amount actually disbursed for this cycle: gross - income tax - employee pension. */
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    /** Gross salary this cycle was computed from. */
    @Column(name = "gross_amount_minor", nullable = false)
    private Long grossAmountMinor;

    /** Progressive (variable) government income tax withheld. */
    @Column(name = "income_tax_minor", nullable = false)
    private Long incomeTaxMinor = 0L;

    /** Employee pension contribution (7% of gross) deducted from the employee. */
    @Column(name = "employee_pension_minor", nullable = false)
    private Long employeePensionMinor = 0L;

    /** Employer pension contribution (11% of gross) paid by the company, not deducted from the employee. */
    @Column(name = "employer_pension_minor", nullable = false)
    private Long employerPensionMinor = 0L;

    @Column(name = "paid_amount_minor")
    private Long paidAmountMinor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "last_reminder_sent_at")
    private Instant lastReminderSentAt;

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
