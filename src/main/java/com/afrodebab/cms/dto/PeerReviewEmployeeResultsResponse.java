package com.afrodebab.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record PeerReviewEmployeeResultsResponse(
        Long employeeId,
        String employeeName,
        String department,
        String role,
        String employmentType,
        Long subOrganizationId,
        String subOrganizationName,
        BigDecimal leadershipScore,
        List<PeerReviewPrincipleAverageResponse> principleAverages,
        List<String> comments
) {
}
