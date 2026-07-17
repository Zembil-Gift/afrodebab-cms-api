package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.OrgPublicInfo;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Public, anonymous lookup of an organization by its URL slug. Backs the per-org
 * browse pages on the frontend ({@code /o/{slug}}) so they can show the org's name
 * and confirm it exists before listing its public jobs/blogs/events.
 */
@Tag(name = "Public - Organization")
@RestController
@RequestMapping("/public/{orgSlug}")
public class OrgPublicController {

    private final OrganizationRepository orgs;

    public OrgPublicController(OrganizationRepository orgs) { this.orgs = orgs; }

    @GetMapping
    public OrgPublicInfo info(@PathVariable String orgSlug) {
        return orgs.findBySlugIgnoreCase(orgSlug)
                .filter(o -> "ACTIVE".equals(o.getStatus()))
                .map(OrgPublicInfo::from)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }
}
