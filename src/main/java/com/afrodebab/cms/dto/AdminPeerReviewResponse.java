package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.PeerReview;

import java.time.Instant;
import java.time.LocalDate;

public record AdminPeerReviewResponse(
        Long id,
        Long periodId,
        String periodName,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long reviewerId,
        String reviewerName,
        Long revieweeId,
        String revieweeName,
        PeerReview.Rating rating,
        String feedback,
        Instant createdAt,
        Instant updatedAt
) {
}
