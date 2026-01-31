package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.ApplyRequest;
import com.afrodebab.cms.dto.ApplyResponse;
import com.afrodebab.cms.service.JobApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public - JobApply")
@RestController
@RequestMapping("/jobs")
public class JobApplyController {

    private final JobApplicationService service;

    public JobApplyController(JobApplicationService service) { this.service = service; }

    @PostMapping("/{id}/apply")
    public ApplyResponse apply(@PathVariable Long id, @Valid @RequestBody ApplyRequest req) {
        return service.apply(id, req);
    }
}

