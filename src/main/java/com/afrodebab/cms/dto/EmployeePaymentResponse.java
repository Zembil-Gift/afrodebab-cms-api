package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeePaymentResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate cycleStartDate,
        LocalDate dueDate,
        Long amountMinor,
        Long paidAmountMinor,
        String status,
        String transactionReference,
        Instant paidAt,
        Instant lastReminderSentAt,
        Instant createdAt,
        Instant updatedAt
) {}
