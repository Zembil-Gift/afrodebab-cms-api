package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.SubOrganizationCreateRequest;
import com.afrodebab.cms.dto.SubOrganizationRenameRequest;
import com.afrodebab.cms.dto.SubOrganizationResponse;
import com.afrodebab.cms.dto.SubOrganizationUpdateRequest;
import com.afrodebab.cms.service.SubOrganizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Manager - Sub Organizations")
@RestController
@RequestMapping("/manager/sub-organizations")
public class SubOrganizationController {

    private final SubOrganizationService subOrganizationService;

    public SubOrganizationController(SubOrganizationService subOrganizationService) {
        this.subOrganizationService = subOrganizationService;
    }

    @GetMapping
    public List<SubOrganizationResponse> list() {
        return subOrganizationService.listAll();
    }

    @GetMapping("/{id}")
    public SubOrganizationResponse get(@PathVariable Long id) {
        return subOrganizationService.getById(id);
    }

    @PostMapping
    public SubOrganizationResponse create(@Valid @RequestBody SubOrganizationCreateRequest req) {
        return subOrganizationService.create(req);
    }

    @PutMapping("/{id}")
    public SubOrganizationResponse update(@PathVariable Long id, @Valid @RequestBody SubOrganizationUpdateRequest req) {
        return subOrganizationService.update(id, req);
    }

    @PatchMapping("/{id}/rename")
    public SubOrganizationResponse rename(@PathVariable Long id, @Valid @RequestBody SubOrganizationRenameRequest req) {
        return subOrganizationService.rename(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        subOrganizationService.delete(id);
    }
}
