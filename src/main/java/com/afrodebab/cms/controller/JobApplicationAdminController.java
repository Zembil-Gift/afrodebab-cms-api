package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.JobApplicationAdminResponse;
import com.afrodebab.cms.service.JobApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - JobApplicationAdmin")
@RestController
@RequestMapping("/admin/job-applications")
public class JobApplicationAdminController {

    private final JobApplicationService service;

    public JobApplicationAdminController(JobApplicationService service) { this.service = service; }

    @GetMapping
    public List<JobApplicationAdminResponse> listAll() {
        return service.listAll();
    }

    @GetMapping("/{jobId}")
    public List<JobApplicationAdminResponse> listByJob(@PathVariable Long jobId) {
        return service.listByJobId(jobId);
    }
}

