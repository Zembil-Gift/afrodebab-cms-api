package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.ViceManagerCreateRequest;
import com.afrodebab.cms.dto.ViceManagerResponse;
import com.afrodebab.cms.dto.ViceManagerUpdateRequest;
import com.afrodebab.cms.service.ViceManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Manager - Vice Managers")
@RestController
@RequestMapping("/manager/vice-managers")
public class ViceManagerAdminController {

    private final ViceManagerService viceManagerService;

    public ViceManagerAdminController(ViceManagerService viceManagerService) {
        this.viceManagerService = viceManagerService;
    }

    @GetMapping
    public List<ViceManagerResponse> list() {
        return viceManagerService.listAll();
    }

    @GetMapping("/{id}")
    public ViceManagerResponse get(@PathVariable Long id) {
        return viceManagerService.getById(id);
    }

    @PostMapping
    public ViceManagerResponse create(@Valid @RequestBody ViceManagerCreateRequest req) {
        return viceManagerService.create(req);
    }

    @PutMapping("/{id}")
    public ViceManagerResponse update(@PathVariable Long id, @Valid @RequestBody ViceManagerUpdateRequest req) {
        return viceManagerService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        viceManagerService.delete(id);
    }
}
