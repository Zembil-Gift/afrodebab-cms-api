package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.OrgProfileUpdateRequest;
import com.afrodebab.cms.dto.OrgResponse;
import com.afrodebab.cms.service.CloudflareR2Service;
import com.afrodebab.cms.service.OrganizationService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * The logged-in manager's own company profile. Reads/writes the organization already put
 * in tenant scope by {@code JwtAuthFilter}, so a manager can only touch their own org.
 * Secured to ROLE_MANAGER by {@code SecurityConfig} ({@code /manager/**}).
 */
@Tag(name = "Manager - Organization Profile")
@RestController
@RequestMapping("/manager/org")
public class ManagerOrgController {

    private final OrganizationService orgService;
    private final CloudflareR2Service r2Service;

    public ManagerOrgController(OrganizationService orgService, CloudflareR2Service r2Service) {
        this.orgService = orgService;
        this.r2Service = r2Service;
    }

    @GetMapping
    public OrgResponse get() {
        return orgService.getMyOrg();
    }

    @PutMapping
    public OrgResponse update(@Valid @RequestBody OrgProfileUpdateRequest req) {
        return orgService.updateMyOrg(req);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrgResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        String url = r2Service.uploadOrgLogo(TenantContext.get(), file);
        return orgService.setLogoUrl(url);
    }

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrgResponse uploadCover(@RequestParam("file") MultipartFile file) {
        String url = r2Service.uploadOrgCover(TenantContext.get(), file);
        return orgService.setCoverImageUrl(url);
    }
}
