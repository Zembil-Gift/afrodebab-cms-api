package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.PlatformStatsResponse;
import com.afrodebab.cms.service.PlatformStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cross-organization metrics for platform admins. Restricted to ROLE_ADMIN by SecurityConfig. */
@Tag(name = "Platform Admin - Stats")
@RestController
@RequestMapping("/admin/stats")
public class PlatformStatsController {

    private final PlatformStatsService service;

    public PlatformStatsController(PlatformStatsService service) {
        this.service = service;
    }

    @GetMapping
    public PlatformStatsResponse stats() {
        return service.overview();
    }
}
