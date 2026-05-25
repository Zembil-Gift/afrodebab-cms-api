package com.afrodebab.cms.dto;

import java.time.LocalDate;

public record PeerReviewPeriodStatusResponse(
        Long id,
        String name,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean submitted
) {
}
