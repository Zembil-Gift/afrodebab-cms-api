package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.GitHubAggregateResponse;
import com.afrodebab.cms.dto.GitHubReportResponse;
import com.afrodebab.cms.service.GitHubTrackerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Employee - GitHub Tracker")
@RestController
@RequestMapping("/employee/me/github")
public class EmployeeGitHubTrackerController {

    private final GitHubTrackerService trackerService;

    public EmployeeGitHubTrackerController(GitHubTrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping("/report")
    public GitHubReportResponse getOwnReport(Authentication authentication) {
        return trackerService.getEmployeeReportByEmail(authentication.getName());
    }

    @GetMapping("/aggregate")
    public List<GitHubAggregateResponse> getOwnAggregate(
            Authentication authentication,
            @RequestParam(defaultValue = "WEEKLY") String periodType
    ) {
        return trackerService.getAggregatedResultsByEmail(authentication.getName(), periodType);
    }
}
