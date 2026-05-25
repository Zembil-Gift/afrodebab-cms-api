package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.TelegramSupportReportResponse;
import com.afrodebab.cms.dto.TelegramSupportSummaryResponse;
import com.afrodebab.cms.dto.TelegramSupportTicketsResponse;
import com.afrodebab.cms.service.TelegramSupportTrackerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Employee - Telegram Support Tracker")
@RestController
@RequestMapping("/employee/me/telegram/support")
public class EmployeeTelegramSupportController {

    private final TelegramSupportTrackerService trackerService;

    public EmployeeTelegramSupportController(TelegramSupportTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/summary")
    public TelegramSupportSummaryResponse getOwnSummary(
            Authentication authentication,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return trackerService.getSummaryByEmail(authentication.getName(), typeGroup, from, to);
    }

    @GetMapping("/tickets")
    public TelegramSupportTicketsResponse getOwnTickets(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize
    ) {
        return trackerService.getTicketsByEmail(authentication.getName(), status, typeGroup, from, to, page, pageSize);
    }

    @GetMapping("/report")
    public TelegramSupportReportResponse getOwnReport(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return trackerService.getEmployeeReportByEmail(authentication.getName(), status, typeGroup, from, to);
    }
}
