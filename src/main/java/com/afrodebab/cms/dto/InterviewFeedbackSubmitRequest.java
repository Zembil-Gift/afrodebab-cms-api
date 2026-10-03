package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.InterviewFeedback;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewFeedbackSubmitRequest(
        @NotNull InterviewFeedback.Recommendation recommendation,
        @NotBlank @Size(max = 10000) String comments
) {}
