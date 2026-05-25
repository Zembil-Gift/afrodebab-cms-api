package com.afrodebab.cms.dto;

import java.time.LocalDate;

public record PeerReviewSelfResultsResponse(
        Long periodId,
        String periodName,
        LocalDate periodStart,
        LocalDate periodEnd,
        PeerReviewEmployeeResultsResponse employee
){
}
