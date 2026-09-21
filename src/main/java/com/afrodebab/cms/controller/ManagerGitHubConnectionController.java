package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.GitHubConnectRequest;
import com.afrodebab.cms.dto.GitHubConnectionResponse;
import com.afrodebab.cms.dto.GitHubOrgDto;
import com.afrodebab.cms.dto.GitHubOrgSelectionRequest;
import com.afrodebab.cms.service.ManagerGitHubConnectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The logged-in manager links their own GitHub account (OAuth) and chooses which organizations
 * to track. Secured to ROLE_MANAGER by {@code SecurityConfig} ({@code /manager/**}); the manager
 * is resolved from the JWT, so a manager only ever touches their own connection.
 */
@Tag(name = "Manager - GitHub Connection")
@RestController
@RequestMapping({"/manager/github/connection", "/vice-manager/github/connection"})
public class ManagerGitHubConnectionController {

    private final ManagerGitHubConnectionService service;

    public ManagerGitHubConnectionController(ManagerGitHubConnectionService service) {
        this.service = service;
    }

    @GetMapping
    public GitHubConnectionResponse status() {
        return service.status();
    }

    @PostMapping
    public GitHubConnectionResponse connect(@Valid @RequestBody GitHubConnectRequest req) {
        return service.connect(req.code());
    }

    @DeleteMapping
    public void disconnect() {
        service.disconnect();
    }

    @GetMapping("/available-orgs")
    public List<GitHubOrgDto> availableOrgs() {
        return service.availableOrgs();
    }

    @PutMapping("/orgs")
    public GitHubConnectionResponse saveOrgs(@Valid @RequestBody GitHubOrgSelectionRequest req) {
        return service.saveOrgs(req.orgs());
    }
}
