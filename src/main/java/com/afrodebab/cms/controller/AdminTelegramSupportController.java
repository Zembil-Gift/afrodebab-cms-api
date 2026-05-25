package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.TelegramSupportReportResponse;
import com.afrodebab.cms.dto.TelegramSupportSummaryResponse;
import com.afrodebab.cms.dto.TelegramSupportTicketsResponse;
import com.afrodebab.cms.service.TelegramSupportTrackerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Telegram Support Tracker")
@RestController
@RequestMapping("/admin/telegram/support")
public class AdminTelegramSupportController {

    private final TelegramSupportTrackerService trackerService;

    public AdminTelegramSupportController(TelegramSupportTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/summary")
    public TelegramSupportSummaryResponse getSummary(
            @RequestParam(required = false) String adminUsername,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return trackerService.getSummary(adminUsername, typeGroup, from, to);
    }

    @GetMapping("/tickets")
    public TelegramSupportTicketsResponse getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String adminUsername,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize
    ) {
        return trackerService.getTickets(adminUsername, status, typeGroup, from, to, page, pageSize);
    }

    @GetMapping("/report/{employeeId}")
    public TelegramSupportReportResponse getEmployeeReport(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return trackerService.getEmployeeReport(employeeId, status, typeGroup, from, to);
    }
}
