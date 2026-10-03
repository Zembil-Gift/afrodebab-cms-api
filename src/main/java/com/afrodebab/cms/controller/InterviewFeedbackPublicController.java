package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.InterviewFeedbackFormResponse;
import com.afrodebab.cms.dto.InterviewFeedbackSubmitRequest;
import com.afrodebab.cms.service.InterviewFeedbackService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * No-login interviewer feedback links. Kept outside /public/** (that filter resolves the org from a
 * URL slug); the org comes from the link's token instead and the request is scoped to it.
 */
@Tag(name = "Public - Interview feedback")
@RestController
@RequestMapping("/interview-feedback")
public class InterviewFeedbackPublicController {

    private final InterviewFeedbackService service;

    public InterviewFeedbackPublicController(InterviewFeedbackService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public InterviewFeedbackFormResponse get(@PathVariable String token) {
        return TenantContext.callAs(service.resolveOrganizationId(token), () -> service.getForm(token));
    }

    @PostMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submit(@PathVariable String token, @Valid @RequestBody InterviewFeedbackSubmitRequest req) {
        TenantContext.callAs(service.resolveOrganizationId(token), () -> {
            service.submit(token, req);
            return null;
        });
    }
}
