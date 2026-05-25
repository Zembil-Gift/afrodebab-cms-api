package com.afrodebab.cms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PeerReviewSubmitRequest(
        @NotNull(message = "revieweeId is required") Long revieweeId,
        @NotNull(message = "periodStart is required") LocalDate periodStart,
        @NotNull(message = "periodEnd is required") LocalDate periodEnd,
        @NotEmpty(message = "ratings are required") List<@Valid PeerReviewRatingInput> ratings
) {
}
