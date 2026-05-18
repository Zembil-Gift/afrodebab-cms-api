package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmailNotificationResponse;
import com.afrodebab.cms.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Email Notifications")
@RestController
@RequestMapping("/admin/email-notifications")
public class EmailNotificationAdminController {
    private final EmailNotificationService emailNotificationService;

    public EmailNotificationAdminController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping("/failed")
    public Page<EmailNotificationResponse> listFailed(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(defaultValue = "createdAt") String sortBy,
                                                      @RequestParam(defaultValue = "desc") String direction) {
        var dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return emailNotificationService.listFailed(pageable);
    }

    @PostMapping("/{id}/retry")
    public EmailNotificationResponse retryNow(@PathVariable Long id) {
        return emailNotificationService.retryFailedNow(id);
    }
}
