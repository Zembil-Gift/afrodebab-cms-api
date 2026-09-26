package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.GoogleConnectRequest;
import com.afrodebab.cms.dto.GoogleConnectionResponse;
import com.afrodebab.cms.service.ManagerGoogleConnectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** The logged-in manager links their own Google account; the manager is resolved from the JWT. */
@Tag(name = "Manager - Google Connection")
@RestController
@RequestMapping({"/manager/google/connection", "/vice-manager/google/connection"})
public class ManagerGoogleConnectionController {

    private final ManagerGoogleConnectionService service;

    public ManagerGoogleConnectionController(ManagerGoogleConnectionService service) {
        this.service = service;
    }

    @GetMapping
    public GoogleConnectionResponse status() {
        return service.status();
    }

    @PostMapping
    public GoogleConnectionResponse connect(@Valid @RequestBody GoogleConnectRequest req) {
        return service.connect(req.code(), req.redirectUri());
    }

    @DeleteMapping
    public void disconnect() {
        service.disconnect();
    }
}
