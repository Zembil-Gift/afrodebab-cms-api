package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.EventCreateRequest;
import com.afrodebab.cms.dto.EventResponse;
import com.afrodebab.cms.dto.EventUpdateRequest;
import com.afrodebab.cms.dto.ImageUploadResponse;
import com.afrodebab.cms.service.CloudflareR2Service;
import com.afrodebab.cms.service.EventService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin - Event")
@RestController
@RequestMapping("/manager/events")
public class EventAdminController {
    private final EventService service;
    private final CloudflareR2Service r2Service;
    public EventAdminController(EventService service, CloudflareR2Service r2Service) {
        this.service = service;
        this.r2Service = r2Service;
    }

    // Uploads a cover image to R2 and returns its URL; the client then saves it as coverImageUrl.
    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadCoverImage(@RequestParam("file") MultipartFile file) {
        return new ImageUploadResponse(r2Service.uploadOrgContentImage(TenantContext.get(), "events", file));
    }

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

