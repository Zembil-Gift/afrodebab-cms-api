package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.JobCreateRequest;
import com.afrodebab.cms.dto.JobResponse;
import com.afrodebab.cms.dto.JobUpdateRequest;
import com.afrodebab.cms.service.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Admin - Jobs")
@RestController
@RequestMapping("/admin/jobs")
public class JobAdminController {
    private final JobService service;
    public JobAdminController(JobService service) { this.service = service; }

    @PostMapping public JobResponse create(@Valid @RequestBody JobCreateRequest req) { return service.create(req); }
    @PutMapping("/{id}") public JobResponse update(@PathVariable Long id, @RequestBody JobUpdateRequest req) { return service.update(id, req); }
}

