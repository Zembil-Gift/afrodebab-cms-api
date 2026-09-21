package com.afrodebab.cms.controller;

import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;
import com.afrodebab.cms.dto.GitHubReportResponse;
import com.afrodebab.cms.dto.TelegramSupportReportResponse;
import com.afrodebab.cms.dto.TrelloReportResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.service.GitHubTrackerService;
import com.afrodebab.cms.service.TelegramSupportTrackerService;
import com.afrodebab.cms.service.TrelloTrackerService;
import com.afrodebab.cms.tenant.SubOrgContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vice Manager - Trackers")
@RestController
@RequestMapping("/vice-manager/trackers")
public class ViceManagerTrackerController {

    private final GitHubTrackerService gitHubTrackerService;
    private final TrelloTrackerService trelloTrackerService;
    private final TelegramSupportTrackerService telegramTrackerService;
    private final EmployeeRepository employeeRepo;
    private final ManagerRepository managerRepo;

    public ViceManagerTrackerController(GitHubTrackerService gitHubTrackerService,
                                        TrelloTrackerService trelloTrackerService,
                                        TelegramSupportTrackerService telegramTrackerService,
                                        EmployeeRepository employeeRepo,
                                        ManagerRepository managerRepo) {
        this.gitHubTrackerService = gitHubTrackerService;
        this.trelloTrackerService = trelloTrackerService;
        this.telegramTrackerService = telegramTrackerService;
        this.employeeRepo = employeeRepo;
        this.managerRepo = managerRepo;
    }

    @GetMapping("/github/report/{employeeId}")
    public GitHubReportResponse getGitHubReport(Authentication auth, @PathVariable Long employeeId) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(employeeId, subOrgId);
        return gitHubTrackerService.getEmployeeReport(employeeId);
    }

    @GetMapping("/trello/report/{employeeId}")
    public TrelloReportResponse getTrelloReport(Authentication auth, @PathVariable Long employeeId) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(employeeId, subOrgId);
        return trelloTrackerService.getEmployeeReport(employeeId);
    }

    @GetMapping("/telegram/report/{employeeId}")
    public TelegramSupportReportResponse getTelegramReport(
            Authentication auth,
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeGroup,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(employeeId, subOrgId);
        return telegramTrackerService.getEmployeeReport(employeeId, status, typeGroup, from, to);
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
