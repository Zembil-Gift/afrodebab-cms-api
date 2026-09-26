package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.InterviewParticipantOptionsResponse;
import com.afrodebab.cms.dto.InterviewRequest;
import com.afrodebab.cms.dto.InterviewResponse;
import com.afrodebab.cms.dto.InterviewStatusRequest;
import com.afrodebab.cms.service.InterviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Manager - Interviews")
@RestController
@RequestMapping("/manager/interviews")
public class InterviewAdminController {

    private final InterviewService service;

    public InterviewAdminController(InterviewService service) {
        this.service = service;
    }

    @GetMapping("/participant-options")
    public InterviewParticipantOptionsResponse participantOptions() {
        return service.participantOptions();
    }

    @GetMapping("/job/{jobId}")
    public List<InterviewResponse> listForJob(@PathVariable Long jobId) {
        return service.listForJob(jobId);
    }

    @GetMapping("/application/{applicationId}")
    public List<InterviewResponse> listForApplication(@PathVariable Long applicationId) {
        return service.listForApplication(applicationId);
    }

    @PostMapping("/application/{applicationId}")
    public InterviewResponse schedule(@PathVariable Long applicationId, @Valid @RequestBody InterviewRequest req) {
        return service.schedule(applicationId, req);
    }

    @PutMapping("/{id}")
    public InterviewResponse reschedule(@PathVariable Long id, @Valid @RequestBody InterviewRequest req) {
        return service.reschedule(id, req);
    }

    @PostMapping("/{id}/cancel")
    public InterviewResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/status")
    public InterviewResponse setOutcome(@PathVariable Long id, @Valid @RequestBody InterviewStatusRequest req) {
        return service.setOutcome(id, req.status());
    }
}
