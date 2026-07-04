package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Manager-facing payroll view. Exposes the full breakdown, including the government income
 * tax and the employer-paid 11% pension, which the company is responsible for remitting.
 */
public record EmployeePaymentResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate cycleStartDate,
        LocalDate dueDate,
        Long amountMinor,
        Long grossAmountMinor,
        Long incomeTaxMinor,
        Long employeePensionMinor,
        Long employerPensionMinor,
        Long retirementSavingMinor,
        Long paidAmountMinor,
        String status,
        String transactionReference,
        Instant paidAt,
        Instant lastReminderSentAt,
        Instant createdAt,
        Instant updatedAt
) {}
