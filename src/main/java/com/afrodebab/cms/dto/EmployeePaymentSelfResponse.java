package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Employee-facing payroll view. Shows gross, net, the tax deducted from the employee
 * (variable income tax + the 7% pension) and the total retirement saving (7% + 11%).
 * The government income tax and employer 11% are not broken out here.
 */
public record EmployeePaymentSelfResponse(
        Long id,
        LocalDate cycleStartDate,
        LocalDate dueDate,
        Long grossAmountMinor,
        Long netAmountMinor,
        Long taxMinor,
        Long retirementSavingMinor,
        Long paidAmountMinor,
        String status,
        String transactionReference,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {}
