package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.PeerReview;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPeerReviewUpsertRequest(
        @NotNull(message = "rating is required") PeerReview.Rating rating,
        @NotBlank(message = "feedback is required") String feedback
) {
}
