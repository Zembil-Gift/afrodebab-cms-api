package com.afrodebab.cms.controller;

import org.springframework.web.bind.annotation.PostMapping;
import com.afrodebab.cms.dto.PeerReviewEmployeeSummaryResponse;
import com.afrodebab.cms.dto.PeerReviewResponse;
import com.afrodebab.cms.dto.EmployeeMetricSummaryResponse;
import com.afrodebab.cms.dto.EmployeeTimeSpentResponse;
import com.afrodebab.cms.dto.PeerReviewEmployeeResultsResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodResultsResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.service.EmployeeTimeSpentService;
import com.afrodebab.cms.service.MetricsService;
import com.afrodebab.cms.service.PeerReviewService;
import com.afrodebab.cms.tenant.SubOrgContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Vice Manager - Metrics & Reviews")
@RestController
@RequestMapping("/vice-manager/metrics")
public class ViceManagerMetricsController {

    private final MetricsService metricsService;
    private final PeerReviewService peerReviewService;
    private final EmployeeTimeSpentService employeeTimeSpentService;
    private final EmployeeRepository employeeRepo;
    private final ManagerRepository managerRepo;

    public ViceManagerMetricsController(MetricsService metricsService,
                                        PeerReviewService peerReviewService,
                                        EmployeeTimeSpentService employeeTimeSpentService,
                                        EmployeeRepository employeeRepo,
                                        ManagerRepository managerRepo) {
        this.metricsService = metricsService;
        this.peerReviewService = peerReviewService;
        this.employeeTimeSpentService = employeeTimeSpentService;
        this.employeeRepo = employeeRepo;
        this.managerRepo = managerRepo;
    }


    @GetMapping("/employees")
    public Page<EmployeeMetricSummaryResponse> employeeMetrics(
            Authentication auth,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
                PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return metricsService.getEmployeeMetricsPage(periodStart, periodEnd, department, role, subOrgId, pageable, false);
    }

    @GetMapping("/employees/{id}")
    public EmployeeMetricSummaryResponse singleEmployeeMetrics(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(id, subOrgId);
        return metricsService.getEmployeeMetrics(id, periodStart, periodEnd, false);
    }

    @GetMapping("/peer-reviews/periods")
    public List<PeerReviewPeriodResponse> listPeriods() {
        return peerReviewService.listInitiatedPeriods();
    }

    @GetMapping("/peer-reviews")
    public List<PeerReviewResponse> peerReviews(Authentication auth,
                                                @RequestParam LocalDate periodStart,
                                                @RequestParam LocalDate periodEnd,
                                                @RequestParam(required = false) Long revieweeId) {
        return peerReviewService.listByPeriod(periodStart, periodEnd, revieweeId, resolveSubOrgId(auth));
    }

    @GetMapping("/peer-reviews/summary")
    public List<PeerReviewEmployeeSummaryResponse> peerReviewSummary(Authentication auth,
                                                                     @RequestParam LocalDate periodStart,
                                                                     @RequestParam LocalDate periodEnd,
                                                                     @RequestParam(required = false) Long revieweeId) {
        return peerReviewService.summarizeByEmployee(periodStart, periodEnd, revieweeId, resolveSubOrgId(auth));
    }

    @PostMapping("/employees/{id}/snapshot")
    public EmployeeMetricSummaryResponse refreshSnapshot(Authentication auth,
                                                         @PathVariable Long id,
                                                         @RequestParam LocalDate periodStart,
                                                         @RequestParam LocalDate periodEnd) {
        verifyEmployeeInSubOrg(id, resolveSubOrgId(auth));
        return metricsService.refreshSnapshot(id, periodStart, periodEnd);
    }

    @GetMapping("/peer-reviews/periods/{id}/results")
    public PeerReviewPeriodResultsResponse getPeriodResults(Authentication auth, @PathVariable Long id) {
        Long subOrgId = resolveSubOrgId(auth);
        return peerReviewService.getPeriodResults(id, subOrgId);
    }

    @GetMapping("/peer-reviews/periods/{id}/comments/{employeeId}")
    public List<String> getEmployeeComments(
            Authentication auth,
            @PathVariable Long id,
            @PathVariable Long employeeId
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(employeeId, subOrgId);
        return peerReviewService.getEmployeePeriodComments(id, employeeId);
    }

    @GetMapping("/employees/{id}/time-spent/daily")
    public EmployeeTimeSpentResponse dailyTimeSpent(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(id, subOrgId);
        return employeeTimeSpentService.getEmployeeDaily(id, date);
    }

    @GetMapping("/employees/{id}/time-spent/weekly")
    public EmployeeTimeSpentResponse weeklyTimeSpent(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(id, subOrgId);
        return employeeTimeSpentService.getEmployeeWeekly(id, date);
    }

    @GetMapping("/employees/{id}/time-spent/monthly")
    public EmployeeTimeSpentResponse monthlyTimeSpent(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(id, subOrgId);
        return employeeTimeSpentService.getEmployeeMonthly(id, date);
    }

    private Long resolveSubOrgId(Authentication auth) {
        Long subOrgId = SubOrgContext.get();
        if (subOrgId != null) return subOrgId;
        Manager manager = managerRepo.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NotFoundException("Vice Manager not found"));
        if (manager.getSubOrganization() == null) {
            throw new BadRequestException("No sub-organization assigned to this Vice Manager");
        }
        return manager.getSubOrganization().getId();
    }

    private Employee verifyEmployeeInSubOrg(Long employeeId, Long subOrgId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        if (employee.getSubOrganization() == null || !employee.getSubOrganization().getId().equals(subOrgId)) {
            throw new BadRequestException("Employee does not belong to your sub-organization");
        }
        return employee;
    }
}
