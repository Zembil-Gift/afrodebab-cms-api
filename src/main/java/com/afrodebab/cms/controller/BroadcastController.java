package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.BroadcastPreviewResponse;
import com.afrodebab.cms.dto.BroadcastRequest;
import com.afrodebab.cms.dto.BroadcastResponse;
import com.afrodebab.cms.service.BroadcastService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Managers broadcast org-wide or per branch; vice managers are pinned to their branch by the service. */
@Tag(name = "Manager - Broadcasts")
@RestController
@RequestMapping({"/manager/broadcasts", "/vice-manager/broadcasts"})
public class BroadcastController {

    private final BroadcastService service;

    public BroadcastController(BroadcastService service) {
        this.service = service;
    }

    @GetMapping
    public Page<BroadcastResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    public BroadcastResponse send(@Valid @RequestBody BroadcastRequest req) {
        return service.send(req);
    }

    @PostMapping("/preview")
    public BroadcastPreviewResponse preview(@Valid @RequestBody BroadcastRequest req) {
        return service.preview(req);
    }

    @GetMapping("/recipient-count")
    public Map<String, Integer> recipientCount(@RequestParam(required = false) List<Long> subOrganizationIds) {
        return Map.of("count", service.recipientCount(subOrganizationIds));
    }
}
