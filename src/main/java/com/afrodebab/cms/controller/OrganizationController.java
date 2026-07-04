package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.OrgCreateRequest;
import com.afrodebab.cms.dto.OrgResponse;
import com.afrodebab.cms.service.OrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Global organization CRUD. Restricted to platform admins (ROLE_ADMIN) by SecurityConfig. */
@Tag(name = "Platform Admin - Organizations")
@RestController
@RequestMapping("/admin/orgs")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    public OrgResponse create(@Valid @RequestBody OrgCreateRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<OrgResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public OrgResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/suspend")
    public OrgResponse suspend(@PathVariable Long id) {
        return service.setStatus(id, "SUSPENDED");
    }

    @PostMapping("/{id}/activate")
    public OrgResponse activate(@PathVariable Long id) {
        return service.setStatus(id, "ACTIVE");
    }
}
