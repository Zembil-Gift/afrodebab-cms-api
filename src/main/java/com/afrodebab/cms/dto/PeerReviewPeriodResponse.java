package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PeerReviewPeriodResponse(
        Long id,
        String name,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant createdAt
) {
}
