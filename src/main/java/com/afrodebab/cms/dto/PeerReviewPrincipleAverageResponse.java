package com.afrodebab.cms.dto;

import java.math.BigDecimal;

public record PeerReviewPrincipleAverageResponse(
        Long principleId,
        String principleName,
        BigDecimal averageRating,
        long ratingCount
) {
}
