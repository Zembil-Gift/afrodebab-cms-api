package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.GitHubConnectionResponse;
import com.afrodebab.cms.dto.GitHubOrgDto;
import com.afrodebab.cms.jpa.entity.GitHubOrgRef;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.security.TokenCipher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Per-manager GitHub account linking via OAuth. A logged-in manager authorizes our GitHub
 * OAuth App; the frontend hands back the {@code code}, which we exchange server-side (with
 * the client secret) for an access token, then the manager picks which orgs to track. All
 * request-scoped methods operate on the manager resolved from the JWT, already tenant scoped
 * by {@code JwtAuthFilter}. Tokens are encrypted at rest via {@link TokenCipher}.
 */
@Service
public class ManagerGitHubConnectionService {

    private final ManagerRepository managerRepo;
    private final TokenCipher cipher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.github.client-id:}")
    private String clientId;

    @Value("${app.github.client-secret:}")
    private String clientSecret;

    public ManagerGitHubConnectionService(ManagerRepository managerRepo,
                                          TokenCipher cipher,
                                          ObjectMapper objectMapper) {
        this.managerRepo = managerRepo;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Reference to a connected manager, used by the sync orchestrator (see GitHubTrackerService). */
    public record ManagerRef(Long managerId, Long organizationId) {}

    @Transactional
    public GitHubConnectionResponse connect(String code) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "GitHub integration is not configured (GITHUB_CLIENT_ID/SECRET missing)");
        }
        String token = exchangeCodeForToken(code);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub authorization code");
        }
        Manager manager = currentManager();
        manager.setGithubToken(cipher.encrypt(token));
        managerRepo.save(manager);
        return toResponse(manager);
    }

    @Transactional(readOnly = true)
    public GitHubConnectionResponse status() {
        return toResponse(currentManager());
    }

    /** Live list of the manager's GitHub orgs, so the frontend can present a picker. */
    @Transactional(readOnly = true)
    public List<GitHubOrgDto> availableOrgs() {
        Manager manager = currentManager();
        String token = requireToken(manager);
        return fetchOrgs(token);
    }

    @Transactional
    public GitHubConnectionResponse saveOrgs(List<GitHubOrgDto> orgs) {
        Manager manager = currentManager();
        requireToken(manager);
        manager.getGithubOrgs().clear();
        for (GitHubOrgDto o : orgs) {
            if (o.login() == null || o.login().isBlank()) continue;
            manager.getGithubOrgs().add(new GitHubOrgRef(o.login(), o.name()));
        }
        managerRepo.save(manager);
        return toResponse(manager);
    }

    @Transactional
    public void disconnect() {
        Manager manager = currentManager();
        manager.setGithubToken(null);
        manager.getGithubOrgs().clear();
        managerRepo.save(manager);
    }

    /** Root-scoped: every active manager who has connected GitHub, across all orgs. */
    @Transactional(readOnly = true)
    public List<ManagerRef> findConnectedManagers() {
        return managerRepo.findAllByActiveTrueAndGithubTokenIsNotNull().stream()
                .map(m -> new ManagerRef(m.getId(), m.getOrganizationId()))
                .toList();
    }

    private Manager currentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private String requireToken(Manager manager) {
        if (manager.getGithubToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub is not connected");
        }
        return cipher.decrypt(manager.getGithubToken());
    }

    private GitHubConnectionResponse toResponse(Manager manager) {
        List<GitHubOrgDto> orgs = manager.getGithubOrgs().stream()
                .map(o -> new GitHubOrgDto(o.getOrgLogin(), o.getOrgName()))
                .toList();
        return new GitHubConnectionResponse(manager.getGithubToken() != null, orgs);
    }

    private String exchangeCodeForToken(String code) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "code", code));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/login/oauth/access_token"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "AfroDebab-CMS-API")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;
            JsonNode root = objectMapper.readTree(res.body());
            return root.path("access_token").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private List<GitHubOrgDto> fetchOrgs(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user/orgs?per_page=100"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "AfroDebab-CMS-API")
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "GitHub orgs request failed: " + res.statusCode());
            }
            JsonNode root = objectMapper.readTree(res.body());
            List<GitHubOrgDto> orgs = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode o : root) {
                    String login = o.path("login").asText("");
                    String name = o.path("name").asText("");
                    orgs.add(new GitHubOrgDto(login, name.isBlank() ? login : name));
                }
            }
            return orgs;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach GitHub", e);
        }
    }
}
