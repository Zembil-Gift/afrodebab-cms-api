package com.afrodebab.cms.dto;

import java.time.LocalDate;
import java.util.List;

public record PeerReviewPeriodResultsResponse(
        Long periodId,
        String periodName,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<PeerReviewEmployeeResultsResponse> employees
) {
}
