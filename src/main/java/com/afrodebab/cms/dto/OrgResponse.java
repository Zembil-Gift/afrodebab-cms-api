package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Organization;

import java.time.Instant;

/**
 * Full organization view for platform admins and for the org's own manager
 * (backs the "Company Profile" editor at {@code /manager/org}).
 */
public record OrgResponse(
        Long id,
        String name,
        String slug,
        String status,
        String plan,
        String tagline,
        String description,
        String logoUrl,
        String coverImageUrl,
        String businessType,
        String industry,
        String companySize,
        Integer foundedYear,
        String phone,
        String companyEmail,
        String websiteUrl,
        String addressLine,
        String city,
        String country,
        String linkedinUrl,
        String twitterUrl,
        String facebookUrl,
        String instagramUrl,
        Instant createdAt
) {
    public static OrgResponse from(Organization o) {
        return new OrgResponse(
                o.getId(), o.getName(), o.getSlug(), o.getStatus(), o.getPlan(),
                o.getTagline(), o.getDescription(), o.getLogoUrl(), o.getCoverImageUrl(),
                o.getBusinessType(), o.getIndustry(), o.getCompanySize(), o.getFoundedYear(),
                o.getPhone(), o.getCompanyEmail(), o.getWebsiteUrl(),
                o.getAddressLine(), o.getCity(), o.getCountry(),
                o.getLinkedinUrl(), o.getTwitterUrl(), o.getFacebookUrl(), o.getInstagramUrl(),
                o.getCreatedAt());
    }
}
