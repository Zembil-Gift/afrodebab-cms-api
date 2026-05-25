package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.TrelloAggregateResponse;
import com.afrodebab.cms.dto.TrelloReportResponse;
import com.afrodebab.cms.service.TrelloTrackerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Employee - Trello Tracker")
@RestController
@RequestMapping("/employee/me/trello")
public class EmployeeTrelloTrackerController {

    private final TrelloTrackerService trackerService;

    public EmployeeTrelloTrackerController(TrelloTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/report")
    public TrelloReportResponse getOwnReport(Authentication authentication) {
        return trackerService.getEmployeeReportByEmail(authentication.getName());
    }

    @GetMapping("/aggregate")
    public List<TrelloAggregateResponse> getOwnAggregate(
            Authentication authentication,
            @RequestParam(defaultValue = "WEEKLY") String periodType
    ) {
        return trackerService.getAggregatedResultsByEmail(authentication.getName(), periodType);
    }
}
