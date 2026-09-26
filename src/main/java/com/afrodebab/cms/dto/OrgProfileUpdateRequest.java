package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Manager-editable company profile. Applied to the manager's own organization
 * ({@code PUT /manager/org}). The slug, plan and status are NOT editable here.
 * Logo and cover images are set separately via the upload endpoints or as URLs.
 */
public record OrgProfileUpdateRequest(
        @NotBlank(message = "company name is required") @Size(max = 255) String name,
        @Size(max = 255) String tagline,
        @Size(max = 20000) String description,
        @Size(max = 1024) String logoUrl,
        @Size(max = 1024) String coverImageUrl,
        @Size(max = 120) String businessType,
        @Size(max = 120) String industry,
        @Size(max = 60) String companySize,
        Integer foundedYear,
        @Size(max = 60) String phone,
        @Email(message = "company email must be valid") @Size(max = 200) String companyEmail,
        @Size(max = 512) String websiteUrl,
        @Size(max = 255) String addressLine,
        @Size(max = 120) String city,
        @Size(max = 120) String country,
        @Size(max = 512) String linkedinUrl,
        @Size(max = 512) String twitterUrl,
        @Size(max = 512) String facebookUrl,
        @Size(max = 512) String instagramUrl
) {}
