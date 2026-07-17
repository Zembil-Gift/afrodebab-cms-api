package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.TrelloBoardDto;
import com.afrodebab.cms.dto.TrelloBoardSelectionRequest;
import com.afrodebab.cms.dto.TrelloConnectRequest;
import com.afrodebab.cms.dto.TrelloConnectionResponse;
import com.afrodebab.cms.service.ManagerTrelloConnectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The logged-in manager links their own Trello account and chooses which boards to track.
 * Secured to ROLE_MANAGER by {@code SecurityConfig} ({@code /manager/**}); the manager is
 * resolved from the JWT, so a manager only ever touches their own connection.
 */
@Tag(name = "Manager - Trello Connection")
@RestController
@RequestMapping("/manager/trello/connection")
public class ManagerTrelloConnectionController {

    private final ManagerTrelloConnectionService service;

    public ManagerTrelloConnectionController(ManagerTrelloConnectionService service) {
        this.service = service;
    }

    @GetMapping
    public TrelloConnectionResponse status() {
        return service.status();
    }

    @PostMapping
    public TrelloConnectionResponse connect(@Valid @RequestBody TrelloConnectRequest req) {
        return service.connect(req.token());
    }

    @DeleteMapping
    public void disconnect() {
        service.disconnect();
    }

    @GetMapping("/available-boards")
    public List<TrelloBoardDto> availableBoards() {
        return service.availableBoards();
    }

    @PutMapping("/boards")
    public TrelloConnectionResponse saveBoards(@Valid @RequestBody TrelloBoardSelectionRequest req) {
        return service.saveBoards(req.boards());
    }
}
