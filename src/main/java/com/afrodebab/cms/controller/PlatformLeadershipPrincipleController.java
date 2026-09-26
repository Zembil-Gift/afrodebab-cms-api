package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.LeadershipPrincipleRequest;
import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.service.DefaultLeadershipPrincipleService;
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

/** The default principle set organizations copy from; each org then manages its own copy. */
@Tag(name = "Platform Admin - Leadership Principles")
@RestController
@RequestMapping("/admin/principles")
public class PlatformLeadershipPrincipleController {

    private final DefaultLeadershipPrincipleService service;

    public PlatformLeadershipPrincipleController(DefaultLeadershipPrincipleService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeadershipPrincipleResponse> list() {
        return service.listAll();
    }

    @PostMapping
    public LeadershipPrincipleResponse create(@Valid @RequestBody LeadershipPrincipleRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public LeadershipPrincipleResponse update(@PathVariable Long id, @Valid @RequestBody LeadershipPrincipleRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
