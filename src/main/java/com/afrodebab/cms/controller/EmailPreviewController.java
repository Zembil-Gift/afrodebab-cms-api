package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmailPreviewRequest;
import com.afrodebab.cms.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Public - Email Preview")
@RestController
@RequestMapping("/email-preview")
public class EmailPreviewController {
    private final EmailTemplateService emailTemplateService;

    public EmailPreviewController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping("/cases")
    public List<String> listEmailCases() {
        return emailTemplateService.sampleCases();
    }

    @PostMapping("/send")
    public Map<String, String> sendPreview(@Valid @RequestBody EmailPreviewRequest req) {
        String email = req.email().trim();
        emailTemplateService.sendSample(req.emailCase(), email);
        return Map.of(
                "message", "Preview email sent",
                "email", email,
                "emailCase", req.emailCase().trim()
        );
    }
}
