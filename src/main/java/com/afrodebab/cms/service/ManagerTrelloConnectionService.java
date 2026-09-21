package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.TrelloBoardDto;
import com.afrodebab.cms.dto.TrelloConnectionResponse;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.SubOrganization;
import com.afrodebab.cms.jpa.entity.TrelloBoardRef;
import com.afrodebab.cms.jpa.entity.TrelloBoardSubOrg;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-manager Trello account linking. A logged-in manager connects their own Trello token
 * (obtained via the frontend authorize flow) and picks which of their boards to track.
 * All request-scoped methods operate on the manager resolved from the JWT, already tenant
 * scoped by {@code JwtAuthFilter}. Tokens are encrypted at rest via {@link TokenCipher}.
 */
@Service
public class ManagerTrelloConnectionService {

    private final ManagerRepository managerRepo;
    private final TokenCipher cipher;
    private final SubOrganizationService subOrganizationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${TRELLO_API:}")
    private String trelloKey;

    public ManagerTrelloConnectionService(ManagerRepository managerRepo,
                                          TokenCipher cipher,
                                          SubOrganizationService subOrganizationService,
                                          ObjectMapper objectMapper) {
        this.managerRepo = managerRepo;
        this.cipher = cipher;
        this.subOrganizationService = subOrganizationService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Reference to a connected manager, used by the sync orchestrator (see TrelloTrackerService). */
    public record ManagerRef(Long managerId, Long organizationId) {}

    @Transactional
    public TrelloConnectionResponse connect(String token) {
        if (trelloKey == null || trelloKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Trello integration is not configured (TRELLO_API missing)");
        }
        if (!validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Trello token");
        }
        Manager manager = currentManager();
        manager.setTrelloToken(cipher.encrypt(token));
        managerRepo.save(manager);
        return toResponse(manager);
    }

    @Transactional(readOnly = true)
    public TrelloConnectionResponse status() {
        return toResponse(currentManager());
    }

    /** Live list of the manager's Trello boards, so the frontend can present a picker. */
    @Transactional(readOnly = true)
    public List<TrelloBoardDto> availableBoards() {
        Manager manager = currentManager();
        String token = requireToken(manager);
        return fetchBoards(token);
    }

    @Transactional
    public TrelloConnectionResponse saveBoards(List<TrelloBoardDto> boards) {
        Manager manager = currentManager();
        requireToken(manager);
        // Vice managers always credit their own branch (applied at sync), so only managers pick.
        boolean pickBranches = manager.getRole() != Manager.ManagerRole.VICE_MANAGER;
        manager.getTrelloBoards().clear();
        manager.getTrelloBoardSubOrgs().clear();
        for (TrelloBoardDto b : boards) {
            if (b.id() == null || b.id().isBlank()) continue;
            manager.getTrelloBoards().add(new TrelloBoardRef(b.id(), b.name()));
            if (pickBranches && b.subOrganizationIds() != null) {
                for (Long subOrgId : b.subOrganizationIds()) {
                    subOrganizationService.getEntityOrThrow(subOrgId); // must belong to this org
                    manager.getTrelloBoardSubOrgs().add(new TrelloBoardSubOrg(b.id(), subOrgId));
                }
            }
        }
        managerRepo.save(manager);
        return toResponse(manager);
    }

    @Transactional
    public void disconnect() {
        Manager manager = currentManager();
        manager.setTrelloToken(null);
        manager.getTrelloBoards().clear();
        manager.getTrelloBoardSubOrgs().clear();
        managerRepo.save(manager);
    }

    /** Root-scoped: every active manager who has connected Trello, across all orgs. */
    @Transactional(readOnly = true)
    public List<ManagerRef> findConnectedManagers() {
        return managerRepo.findAllByActiveTrueAndTrelloTokenIsNotNull().stream()
                .map(m -> new ManagerRef(m.getId(), m.getOrganizationId()))
                .toList();
    }

    private Manager currentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private String requireToken(Manager manager) {
        if (manager.getTrelloToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trello is not connected");
        }
        return cipher.decrypt(manager.getTrelloToken());
    }

    private TrelloConnectionResponse toResponse(Manager manager) {
        List<TrelloBoardDto> boards = manager.getTrelloBoards().stream()
                .map(b -> new TrelloBoardDto(b.getBoardId(), b.getBoardName(), manager.getTrelloBoardSubOrgs().stream()
                        .filter(s -> s.getBoardId().equals(b.getBoardId()))
                        .map(TrelloBoardSubOrg::getSubOrganizationId)
                        .sorted()
                        .toList()))
                .toList();
        boolean vice = manager.getRole() == Manager.ManagerRole.VICE_MANAGER;
        SubOrganization branch = vice ? manager.getSubOrganization() : null;
        return new TrelloConnectionResponse(manager.getTrelloToken() != null, boards,
                branch == null ? null : branch.getId(), branch == null ? null : branch.getName(), vice);
    }

    private boolean validateToken(String token) {
        try {
            String url = "https://api.trello.com/1/members/me?fields=id&key="
                    + encode(trelloKey) + "&token=" + encode(token);
            HttpResponse<String> res = httpClient.send(get(url), HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private List<TrelloBoardDto> fetchBoards(String token) {
        try {
            String url = "https://api.trello.com/1/members/me/boards?fields=name&key="
                    + encode(trelloKey) + "&token=" + encode(token);
            HttpResponse<String> res = httpClient.send(get(url), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Trello boards request failed: " + res.statusCode());
            }
            JsonNode root = objectMapper.readTree(res.body());
            List<TrelloBoardDto> boards = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode b : root) {
                    boards.add(new TrelloBoardDto(b.path("id").asText(), b.path("name").asText(""), null));
                }
            }
            return boards;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach Trello", e);
        }
    }

    private HttpRequest get(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "AfroDebab-CMS-API")
                .GET()
                .build();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
