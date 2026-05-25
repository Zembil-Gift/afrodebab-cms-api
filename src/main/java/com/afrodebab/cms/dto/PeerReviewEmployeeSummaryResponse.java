package com.afrodebab.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeerReviewEmployeeSummaryResponse(
        Long employeeId,
        String employeeName,
        String department,
        String role,
        String employmentType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int totalPoints,
        int ratingCount,
        int maxPoints,
        BigDecimal leadershipScore
) {
}
