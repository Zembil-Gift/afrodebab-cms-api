package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.AdminPeerReviewResponse;
import com.afrodebab.cms.dto.AdminPeerReviewUpsertRequest;
import com.afrodebab.cms.dto.EmployeeMetricSummaryResponse;
import com.afrodebab.cms.dto.EmployeeTimeSpentResponse;
import com.afrodebab.cms.dto.PeerReviewAvailableEmployeeResponse;
import com.afrodebab.cms.dto.PeerReviewEmployeeSummaryResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodCreateRequest;
import com.afrodebab.cms.dto.PeerReviewPeriodResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodResultsResponse;
import com.afrodebab.cms.dto.PeerReviewResponse;
import com.afrodebab.cms.service.AdminPeerReviewService;
import com.afrodebab.cms.service.EmployeeTimeSpentService;
import com.afrodebab.cms.service.MetricsService;
import com.afrodebab.cms.service.PeerReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Admin - Metrics")
@RestController
@RequestMapping({"/admin/metrics", "/api/admin/metrics"})
public class AdminMetricsController {
    private final MetricsService metricsService;
    private final PeerReviewService peerReviewService;
    private final AdminPeerReviewService adminPeerReviewService;
    private final EmployeeTimeSpentService employeeTimeSpentService;

    public AdminMetricsController(MetricsService metricsService,
                                  PeerReviewService peerReviewService,
                                  AdminPeerReviewService adminPeerReviewService,
                                  EmployeeTimeSpentService employeeTimeSpentService) {
        this.metricsService = metricsService;
        this.peerReviewService = peerReviewService;
        this.adminPeerReviewService = adminPeerReviewService;
        this.employeeTimeSpentService = employeeTimeSpentService;
    }

    private String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @GetMapping("/employees")
    public Page<EmployeeMetricSummaryResponse> employeeMetrics(
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "false") boolean persistSnapshot
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String dbSortBy = toSnakeCase(sortBy);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, dbSortBy));
        return metricsService.getEmployeeMetricsPage(
                periodStart,
                periodEnd,
                department,
                role,
                pageable,
                persistSnapshot
        );
    }

    @GetMapping("/employees/{employeeId}")
    public EmployeeMetricSummaryResponse employeeMetric(
            @PathVariable Long employeeId,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(defaultValue = "false") boolean persistSnapshot
    ) {
        return metricsService.getEmployeeMetrics(employeeId, periodStart, periodEnd, persistSnapshot);
    }

    @PostMapping("/employees/{employeeId}/snapshot")
    public EmployeeMetricSummaryResponse refreshSnapshot(
            @PathVariable Long employeeId,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd
    ) {
        return metricsService.refreshSnapshot(employeeId, periodStart, periodEnd);
    }

    @GetMapping("/peer-reviews")
    public List<PeerReviewResponse> peerReviews(
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(required = false) Long revieweeId
    ) {
        return peerReviewService.listByPeriod(periodStart, periodEnd, revieweeId);
    }

    @GetMapping("/peer-reviews/summary")
    public List<PeerReviewEmployeeSummaryResponse> peerReviewSummaryByEmployee(
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(required = false) Long revieweeId
    ) {
        return peerReviewService.summarizeByEmployee(periodStart, periodEnd, revieweeId);
    }

    @PostMapping("/peer-reviews/periods/{periodId}/admin-reviews/{employeeId}")
    public AdminPeerReviewResponse upsertAdminPeerReview(
            Authentication authentication,
            @PathVariable Long periodId,
            @PathVariable Long employeeId,
            @Valid @RequestBody AdminPeerReviewUpsertRequest request
    ) {
        return adminPeerReviewService.upsert(authentication.getName(), periodId, employeeId, request);
    }

    @GetMapping("/peer-reviews/periods/{periodId}/admin-reviews/{employeeId}")
    public AdminPeerReviewResponse getAdminPeerReview(
            @PathVariable Long periodId,
            @PathVariable Long employeeId
    ) {
        return adminPeerReviewService.getForAdmin(periodId, employeeId);
    }

    @GetMapping("/peer-reviews/periods")
    public List<PeerReviewPeriodResponse> initiatedPeerReviewPeriods() {
        return peerReviewService.listInitiatedPeriods();
    }

    @PostMapping("/peer-reviews/periods")
    public PeerReviewPeriodResponse initiatePeerReviewPeriod(
            @Valid @RequestBody PeerReviewPeriodCreateRequest request
    ) {
        return peerReviewService.initiatePeriod(request);
    }

    @GetMapping("/peer-reviews/periods/{periodId}/results")
    public PeerReviewPeriodResultsResponse peerReviewResults(@PathVariable Long periodId) {
        return peerReviewService.getPeriodResults(periodId);
    }

    @GetMapping("/peer-reviews/available-employees")
    public List<PeerReviewAvailableEmployeeResponse> availableEmployees(Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return peerReviewService.listAvailableEmployeesForAdmin(adminEmail);
    }

    @GetMapping("/employees/{employeeId}/time-spent/daily")
    public EmployeeTimeSpentResponse employeeDailyTimeSpent(
            @PathVariable Long employeeId,
            @RequestParam LocalDate date
    ) {
        return employeeTimeSpentService.getEmployeeDaily(employeeId, date);
    }

    @GetMapping("/employees/{employeeId}/time-spent/weekly")
    public EmployeeTimeSpentResponse employeeWeeklyTimeSpent(
            @PathVariable Long employeeId,
            @RequestParam LocalDate date
    ) {
        return employeeTimeSpentService.getEmployeeWeekly(employeeId, date);
    }

    @GetMapping("/employees/{employeeId}/time-spent/monthly")
    public EmployeeTimeSpentResponse employeeMonthlyTimeSpent(
            @PathVariable Long employeeId,
            @RequestParam LocalDate date
    ) {
        return employeeTimeSpentService.getEmployeeMonthly(employeeId, date);
    }
}
