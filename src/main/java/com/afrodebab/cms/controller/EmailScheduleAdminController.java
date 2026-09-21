package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmailScheduleResponse;
import com.afrodebab.cms.dto.EmailScheduleUpdateRequest;
import com.afrodebab.cms.service.EmailScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** When the org's queued emails are sent and how often payroll reminders are queued. */
@Tag(name = "Admin - Email Schedule")
@RestController
@RequestMapping("/manager/email-schedule")
public class EmailScheduleAdminController {
    private final EmailScheduleService emailScheduleService;

    public EmailScheduleAdminController(EmailScheduleService emailScheduleService) {
        this.emailScheduleService = emailScheduleService;
    }

    @GetMapping
    public EmailScheduleResponse get() {
        return emailScheduleService.get();
    }

    @PutMapping
    public EmailScheduleResponse update(@Valid @RequestBody EmailScheduleUpdateRequest req) {
        return emailScheduleService.update(req);
    }
}
