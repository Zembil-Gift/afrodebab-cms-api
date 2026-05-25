package com.afrodebab.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record PeerReviewEmployeeResultsResponse(
        Long employeeId,
        String employeeName,
        String department,
        String role,
        String employmentType,
        BigDecimal leadershipScore,
        List<PeerReviewPrincipleAverageResponse> principleAverages
) {
}
