package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.AdminPeerReviewResponse;
import com.afrodebab.cms.dto.EmployeeMetricSummaryResponse;
import com.afrodebab.cms.dto.EmployeeTimeSpentResponse;
import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.dto.PeerReviewAvailableEmployeeResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodStatusResponse;
import com.afrodebab.cms.dto.PeerReviewResponse;
import com.afrodebab.cms.dto.PeerReviewSelfResultsResponse;
import com.afrodebab.cms.dto.PeerReviewSubmitRequest;
import com.afrodebab.cms.service.AdminPeerReviewService;
import com.afrodebab.cms.service.EmployeeTimeSpentService;
import com.afrodebab.cms.service.MetricsService;
import com.afrodebab.cms.service.PeerReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Employee - Metrics")
@RestController
@RequestMapping("/employee/me")
public class EmployeeMetricsController {
    private final MetricsService metricsService;
    private final PeerReviewService peerReviewService;
    private final AdminPeerReviewService adminPeerReviewService;
    private final EmployeeTimeSpentService employeeTimeSpentService;

    public EmployeeMetricsController(MetricsService metricsService,
                                     PeerReviewService peerReviewService,
                                     AdminPeerReviewService adminPeerReviewService,
                                     EmployeeTimeSpentService employeeTimeSpentService) {
        this.metricsService = metricsService;
        this.peerReviewService = peerReviewService;
        this.adminPeerReviewService = adminPeerReviewService;
        this.employeeTimeSpentService = employeeTimeSpentService;
    }

    @GetMapping("/metrics")
    public EmployeeMetricSummaryResponse ownMetrics(
            Authentication authentication,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(defaultValue = "false") boolean persistSnapshot
    ) {
        return metricsService.getOwnMetrics(authentication.getName(), periodStart, periodEnd, persistSnapshot);
    }

    @GetMapping("/peer-reviews/principles")
    public List<LeadershipPrincipleResponse> principles() {
        return peerReviewService.listActivePrinciples();
    }

    @GetMapping("/peer-reviews/periods")
    public List<PeerReviewPeriodStatusResponse> initiatedPeriods(Authentication authentication) {
        return peerReviewService.listInitiatedPeriodsWithSubmissionStatus(authentication.getName());
    }

    @GetMapping("/peer-reviews/available-employees")
    public List<PeerReviewAvailableEmployeeResponse> availableEmployees(Authentication authentication) {
        return peerReviewService.listAvailableEmployeesForEmployee(authentication.getName());
    }

    @GetMapping("/peer-reviews/periods/{periodId}/results")
    public PeerReviewSelfResultsResponse ownPeerReviewResults(
            Authentication authentication,
            @PathVariable Long periodId
    ) {
        return peerReviewService.getSelfPeriodResults(authentication.getName(), periodId);
    }

    @PostMapping("/peer-reviews")
    public List<PeerReviewResponse> submitPeerReview(
            Authentication authentication,
            @Valid @RequestBody PeerReviewSubmitRequest request
    ) {
        return peerReviewService.submit(authentication.getName(), request);
    }

    @GetMapping("/peer-reviews/received")
    public List<PeerReviewResponse> ownReceivedReviews(
            Authentication authentication,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd
    ) {
        return peerReviewService.listOwnReceived(authentication.getName(), periodStart, periodEnd);
    }

    @GetMapping("/peer-reviews/periods/{periodId}/admin-review")
    public AdminPeerReviewResponse ownAdminPeerReview(
            Authentication authentication,
            @PathVariable Long periodId
    ) {
        return adminPeerReviewService.getForEmployee(authentication.getName(), periodId);
    }

    @GetMapping("/time-spent/daily")
    public EmployeeTimeSpentResponse ownDailyTimeSpent(
            Authentication authentication,
            @RequestParam LocalDate date
    ) {
        return employeeTimeSpentService.getOwnDaily(authentication.getName(), date);
    }

    @GetMapping("/time-spent/weekly")
    public EmployeeTimeSpentResponse ownWeeklyTimeSpent(
            Authentication authentication,
            @RequestParam LocalDate date
    ) {
        return employeeTimeSpentService.getOwnWeekly(authentication.getName(), date);
    }

    @GetMapping("/time-spent/monthly")
    public EmployeeTimeSpentResponse ownMonthlyTimeSpent(
            Authentication authentication,
            @RequestParam LocalDate date
    ) {
        return employeeTimeSpentService.getOwnMonthly(authentication.getName(), date);
    }
}
