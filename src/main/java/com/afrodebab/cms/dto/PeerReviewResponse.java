package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.PeerReview;

import java.time.Instant;
import java.time.LocalDate;

public record PeerReviewResponse(
        Long id,
        Long revieweeId,
        String revieweeName,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long principleId,
        String principleName,
        PeerReview.Rating rating,
        String comment,
        Instant createdAt
) {
}
