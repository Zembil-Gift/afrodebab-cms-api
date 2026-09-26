package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.ManagerMeResponse;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Who am I" for a logged-in manager. Resolves the manager's organization (name + slug)
 * from the tenant already put in scope by {@code JwtAuthFilter}, so the frontend can link
 * to that org's public pages ({@code /o/{slug}}) without hard-coding a slug.
 */
@Tag(name = "Manager - Profile")
@RestController
@RequestMapping("/manager/me")
public class ManagerMeController {

    private final OrganizationRepository orgs;

    public ManagerMeController(OrganizationRepository orgs) { this.orgs = orgs; }

    @GetMapping
    public ManagerMeResponse me(Authentication auth) {
        Long orgId = TenantContext.get();
        Organization org = (orgId == null) ? null : orgs.findById(orgId).orElse(null);
        if (org == null) throw new NotFoundException("Organization not found");
        return new ManagerMeResponse(
                auth != null ? auth.getName() : null,
                org.getId(), org.getName(), org.getSlug()
        );
    }
}
