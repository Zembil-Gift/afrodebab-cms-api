package com.afrodebab.cms.dto;

public record PeerReviewAvailableEmployeeResponse(
        Long id,
        String name,
        String department,
        String role,
        String employmentType
) {
}
