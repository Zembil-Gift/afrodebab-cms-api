package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.service.LeadershipPrincipleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only: principles are platform-wide and edited only under /admin/principles. */
@Tag(name = "Manager - Leadership Principles")
@RestController
@RequestMapping("/manager/metrics/peer-reviews/principles")
public class ManagerLeadershipPrincipleController {

    private final LeadershipPrincipleService service;

    public ManagerLeadershipPrincipleController(LeadershipPrincipleService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeadershipPrincipleResponse> list() {
        return service.listAll();
    }
}
