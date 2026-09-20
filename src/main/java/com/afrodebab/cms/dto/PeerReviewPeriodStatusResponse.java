package com.afrodebab.cms.dto;

import java.time.LocalDate;

public record PeerReviewPeriodStatusResponse(
        Long id,
        String name,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean submitted,
        boolean reviewed,
        int reviewsReceived
) {
    public PeerReviewPeriodStatusResponse(Long id, String name, LocalDate periodStart, LocalDate periodEnd, boolean submitted) {
        this(id, name, periodStart, periodEnd, submitted, false, 0);
    }
}
