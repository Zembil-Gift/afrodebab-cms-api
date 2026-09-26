package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Organization;

/**
 * Public-safe company profile, keyed by URL slug. Backs the single-scroll company
 * page at {@code /o/{slug}} (hero, about, contact, socials).
 */
public record OrgPublicInfo(
        String name,
        String slug,
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
        String instagramUrl
) {
    public static OrgPublicInfo from(Organization o) {
        return new OrgPublicInfo(
                o.getName(), o.getSlug(), o.getPlan(),
                o.getTagline(), o.getDescription(), o.getLogoUrl(), o.getCoverImageUrl(),
                o.getBusinessType(), o.getIndustry(), o.getCompanySize(), o.getFoundedYear(),
                o.getPhone(), o.getCompanyEmail(), o.getWebsiteUrl(),
                o.getAddressLine(), o.getCity(), o.getCountry(),
                o.getLinkedinUrl(), o.getTwitterUrl(), o.getFacebookUrl(), o.getInstagramUrl());
    }
}
