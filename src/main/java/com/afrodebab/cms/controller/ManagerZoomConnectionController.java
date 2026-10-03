package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.GoogleConnectRequest;
import com.afrodebab.cms.dto.ZoomConnectionResponse;
import com.afrodebab.cms.service.ManagerZoomConnectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** The logged-in manager links their own Zoom account; the manager is resolved from the JWT. */
@Tag(name = "Manager - Zoom Connection")
@RestController
@RequestMapping("/manager/zoom/connection")
public class ManagerZoomConnectionController {

    private final ManagerZoomConnectionService service;

    public ManagerZoomConnectionController(ManagerZoomConnectionService service) {
        this.service = service;
    }

    @GetMapping
    public ZoomConnectionResponse status() {
        return service.status();
    }

    @PostMapping
    public ZoomConnectionResponse connect(@Valid @RequestBody GoogleConnectRequest req) {
        return service.connect(req.code(), req.redirectUri());
    }

    @DeleteMapping
    public void disconnect() {
        service.disconnect();
    }
}
