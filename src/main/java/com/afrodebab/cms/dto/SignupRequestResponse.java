package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.SignupRequest;

import java.time.Instant;

public record SignupRequestResponse(
        Long id,
        String companyName,
        String contactName,
        String email,
        String phone,
        String industry,
        String websiteUrl,
        String message,
        String status,
        Instant createdAt
) {
    public static SignupRequestResponse from(SignupRequest r) {
        return new SignupRequestResponse(
                r.getId(),
                r.getCompanyName(),
                r.getContactName(),
                r.getEmail(),
                r.getPhone(),
                r.getIndustry(),
                r.getWebsiteUrl(),
                r.getMessage(),
                r.getStatus().name(),
                r.getCreatedAt()
        );
    }
}
