package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.EventCreateRequest;
import com.afrodebab.cms.dto.EventResponse;
import com.afrodebab.cms.dto.EventUpdateRequest;
import com.afrodebab.cms.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Event")
@RestController
@RequestMapping("/manager/events")
public class EventAdminController {
    private final EventService service;
    public EventAdminController(EventService service) { this.service = service; }

    // Tenant-scoped: returns only the logged-in manager's org events (all statuses).
    @GetMapping
    public Page<EventResponse> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "startDate") String sortBy,
                                    @RequestParam(defaultValue = "desc") String direction) {
        var dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return service.listAllAdmin(PageRequest.of(page, size, Sort.by(dir, sortBy)));
    }

    @GetMapping("/{id}") public EventResponse get(@PathVariable Long id) { return service.getOne(id); }
    @PostMapping public EventResponse create(@Valid @RequestBody EventCreateRequest req) { return service.create(req); }
    @PutMapping("/{id}") public EventResponse update(@PathVariable Long id, @RequestBody EventUpdateRequest req) { return service.update(id, req); }
}

