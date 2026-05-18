package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SelectInterviewCandidatesRequest(
        @NotEmpty(message = "applicationIds is required")
        List<Long> applicationIds
) {}
