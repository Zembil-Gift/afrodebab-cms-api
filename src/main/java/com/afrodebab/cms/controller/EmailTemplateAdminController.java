package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmailPreviewResponse;
import com.afrodebab.cms.dto.EmailTemplateAdminResponse;
import com.afrodebab.cms.dto.EmailTemplateUpdateRequest;
import com.afrodebab.cms.dto.EmailTemplatesAdminResponse;
import com.afrodebab.cms.service.EmailTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Email builder: per-organization subject/heading/message overrides, email logo, preview and test send. */
@Tag(name = "Admin - Email Templates")
@RestController
@RequestMapping("/manager/email-templates")
public class EmailTemplateAdminController {
    private final EmailTemplateService emailTemplateService;

    public EmailTemplateAdminController(EmailTemplateService emailTemplateService) {
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping
    public EmailTemplatesAdminResponse list() {
        return emailTemplateService.list();
    }

    @PutMapping("/{type}")
    public EmailTemplateAdminResponse update(@PathVariable String type, @Valid @RequestBody EmailTemplateUpdateRequest req) {
        return emailTemplateService.update(type, req);
    }

    @DeleteMapping("/{type}")
    public EmailTemplateAdminResponse reset(@PathVariable String type) {
        return emailTemplateService.reset(type);
    }

    @PostMapping("/{type}/preview")
    public EmailPreviewResponse preview(@PathVariable String type, @Valid @RequestBody EmailTemplateUpdateRequest draft) {
        return emailTemplateService.preview(type, draft);
    }

    @PostMapping("/{type}/test")
    public Map<String, String> sendTest(@PathVariable String type,
                                        @Valid @RequestBody EmailTemplateUpdateRequest draft,
                                        Authentication auth) {
        return emailTemplateService.sendTest(type, draft, auth.getName());
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmailTemplatesAdminResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        return emailTemplateService.uploadLogo(file);
    }

    @DeleteMapping("/logo")
    public EmailTemplatesAdminResponse clearLogo() {
        return emailTemplateService.clearLogo();
    }
}
