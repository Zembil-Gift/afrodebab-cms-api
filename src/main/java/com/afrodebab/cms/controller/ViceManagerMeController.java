package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.SubOrganizationResponse;
import com.afrodebab.cms.dto.ViceManagerMeResponse;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.service.SubOrganizationService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Vice Manager - Profile")
@RestController
@RequestMapping("/vice-manager")
public class ViceManagerMeController {

    private final ManagerRepository managerRepo;
    private final OrganizationRepository orgs;
    private final SubOrganizationService subOrganizationService;

    public ViceManagerMeController(ManagerRepository managerRepo, OrganizationRepository orgs,
                                   SubOrganizationService subOrganizationService) {
        this.managerRepo = managerRepo;
        this.orgs = orgs;
        this.subOrganizationService = subOrganizationService;
    }

    /** A vice manager's only branch, in the same shape managers get for their branch filters. */
    @GetMapping("/sub-organizations")
    public List<SubOrganizationResponse> subOrganizations(Authentication auth) {
        Manager manager = managerRepo.findWithSubOrganizationByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NotFoundException("Vice Manager not found"));
        if (manager.getSubOrganization() == null) return List.of();
        return List.of(subOrganizationService.getById(manager.getSubOrganization().getId()));
    }

    @GetMapping("/me")
    public ViceManagerMeResponse me(Authentication auth) {
        String email = auth.getName();
        Manager manager = managerRepo.findWithSubOrganizationByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Vice Manager not found"));

        Long orgId = TenantContext.get();
        Organization org = (orgId == null) ? null : orgs.findById(orgId).orElse(null);
        if (org == null) throw new NotFoundException("Organization not found");

        Long subOrgId = manager.getSubOrganization() != null ? manager.getSubOrganization().getId() : null;
        String subOrgName = manager.getSubOrganization() != null ? manager.getSubOrganization().getName() : null;

        return new ViceManagerMeResponse(
                manager.getEmail(),
                manager.getName(),
                org.getId(),
                org.getName(),
                org.getSlug(),
                subOrgId,
                subOrgName
        );
    }
}
