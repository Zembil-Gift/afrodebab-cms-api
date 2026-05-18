package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.HireCandidateRequest;
import com.afrodebab.cms.dto.JobApplicationStatusUpdateRequest;
import com.afrodebab.cms.dto.SelectInterviewCandidatesRequest;
import com.afrodebab.cms.dto.JobApplicationAdminResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.service.JobApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @PatchMapping("/{id}/status")
    public JobApplicationAdminResponse setStatus(@PathVariable Long id,
                                                 @Valid @RequestBody JobApplicationStatusUpdateRequest req) {
        if (req.status() != JobApplication.ApplicationStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only UNDER_REVIEW is supported by this endpoint");
        }
        return service.setUnderReview(id);
    }

    @PostMapping("/{jobId}/select-interview")
    public List<JobApplicationAdminResponse> selectInterview(@PathVariable Long jobId,
                                                             @Valid @RequestBody SelectInterviewCandidatesRequest req) {
        return service.selectInterviewCandidates(jobId, req.applicationIds());
    }

    @PostMapping("/{jobId}/hire")
    public JobApplicationAdminResponse hire(@PathVariable Long jobId,
                                            @Valid @RequestBody HireCandidateRequest req) {
        return service.hireCandidate(jobId, req);
    }
}
