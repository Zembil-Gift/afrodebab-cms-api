package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.PeerReview;
import jakarta.validation.constraints.NotNull;

public record PeerReviewRatingInput(
        @NotNull(message = "principleId is required") Long principleId,
        @NotNull(message = "rating is required") PeerReview.Rating rating,
        String comment
) {
}
