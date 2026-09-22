package com.afrodebab.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeMetricSummaryResponse(
        Long employeeId,
        String employeeName,
        String role,
        String department,
        String employmentType,
        String employeeStatus,
        Long subOrganizationId,
        String subOrganizationName,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal leadershipScore,
        BigDecimal attendanceScore,
        BigDecimal taskScore,
        BigDecimal overallScore,
        String strengthSummary,
        String improvementSummary
) {
}
