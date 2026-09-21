package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.JobCreateRequest;
import com.afrodebab.cms.dto.JobResponse;
import com.afrodebab.cms.dto.JobUpdateRequest;
import com.afrodebab.cms.service.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Admin - Jobs")
@RestController
@RequestMapping("/manager/jobs")
public class JobAdminController {
    private final JobService service;
    public JobAdminController(JobService service) { this.service = service; }

    // Tenant-scoped: returns only the logged-in manager's org jobs (all statuses).
    @GetMapping
    public Page<JobResponse> list(@RequestParam(required = false) Long subOrganizationId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(defaultValue = "createdAt") String sortBy,
                                  @RequestParam(defaultValue = "desc") String direction) {
        var dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return service.listAll(PageRequest.of(page, size, Sort.by(dir, sortBy)), subOrganizationId);
    }

    @GetMapping("/{id}") public JobResponse get(@PathVariable Long id) { return service.getOne(id); }
    @PostMapping public JobResponse create(@Valid @RequestBody JobCreateRequest req) { return service.create(req); }
    @PutMapping("/{id}") public JobResponse update(@PathVariable Long id, @RequestBody JobUpdateRequest req) { return service.update(id, req); }
}

