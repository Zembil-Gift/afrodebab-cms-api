package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Top-level tenant. Every Manager, Employee and all business data belongs to exactly one
 * Organization. This table is global (not tenant-scoped) and is managed by PlatformAdmins.
 */
@Data
@Entity
@Table(name = "organizations")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** URL-safe identifier used by public endpoints (/public/{slug}/...). */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "plan", nullable = false)
    private String plan = "FREE";

    // --- Public company profile (shown on /o/{slug}, edited by the org's manager) ---

    /** Short headline, e.g. "Payments infrastructure for Africa". */
    @Column private String tagline;

    /** Long-form "about the company" copy. */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")        private String logoUrl;
    @Column(name = "cover_image_url") private String coverImageUrl;

    @Column(name = "business_type")   private String businessType;
    @Column                            private String industry;
    /** Free-form headcount band, e.g. "11-50". */
    @Column(name = "company_size")    private String companySize;
    @Column(name = "founded_year")    private Integer foundedYear;

    @Column                            private String phone;
    @Column(name = "company_email")   private String companyEmail;
    @Column(name = "website_url")     private String websiteUrl;

    @Column(name = "address_line")    private String addressLine;
    @Column                            private String city;
    @Column                            private String country;

    @Column(name = "linkedin_url")    private String linkedinUrl;
    @Column(name = "twitter_url")     private String twitterUrl;
    @Column(name = "facebook_url")    private String facebookUrl;
    @Column(name = "instagram_url")   private String instagramUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
