package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.EventCreateRequest;
import com.afrodebab.cms.dto.EventResponse;
import com.afrodebab.cms.dto.EventUpdateRequest;
import com.afrodebab.cms.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Event")
@RestController
@RequestMapping("/admin/events")
public class EventAdminController {
    private final EventService service;
    public EventAdminController(EventService service) { this.service = service; }

    @PostMapping public EventResponse create(@Valid @RequestBody EventCreateRequest req) { return service.create(req); }
    @PutMapping("/{id}") public EventResponse update(@PathVariable Long id, @RequestBody EventUpdateRequest req) { return service.update(id, req); }
}

