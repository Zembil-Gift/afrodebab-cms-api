package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeeAttendanceResponse;
import com.afrodebab.cms.dto.EmployeeConnectedAccountsAdminResponse;
import com.afrodebab.cms.dto.EmployeeResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.service.EmployeeAttendanceService;
import com.afrodebab.cms.service.EmployeeService;
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

@Tag(name = "Vice Manager - Employees & Attendance")
@RestController
@RequestMapping("/vice-manager/employees")
public class ViceManagerEmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeAttendanceService attendanceService;
    private final EmployeeRepository employeeRepo;
    private final ManagerRepository managerRepo;

    public ViceManagerEmployeeController(EmployeeService employeeService,
                                         EmployeeAttendanceService attendanceService,
                                         EmployeeRepository employeeRepo,
                                         ManagerRepository managerRepo) {
        this.employeeService = employeeService;
        this.attendanceService = attendanceService;
        this.employeeRepo = employeeRepo;
        this.managerRepo = managerRepo;
    }

    @GetMapping
    public Page<EmployeeResponse> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        var dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return employeeService.list(pageable, subOrgId);
    }

    @GetMapping("/connected-accounts")
    public Page<EmployeeConnectedAccountsAdminResponse> connectedAccounts(Authentication auth,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "100") int size) {
        return employeeService.listConnectedAccounts(PageRequest.of(page, size, Sort.by("name")), resolveSubOrgId(auth));
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(Authentication auth, @PathVariable Long id) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(id, subOrgId);
        return employeeService.getById(id);
    }

    @GetMapping("/{id}/attendance")
    public List<EmployeeAttendanceResponse> getAttendanceHistory(Authentication auth, @PathVariable Long id) {
        Long subOrgId = resolveSubOrgId(auth);
        verifyEmployeeInSubOrg(id, subOrgId);
        return attendanceService.history(id);
    }

    @GetMapping("/attendance/date")
    public List<EmployeeAttendanceResponse> getAttendanceByDate(
            Authentication auth,
            @RequestParam LocalDate date
    ) {
        Long subOrgId = resolveSubOrgId(auth);
        return attendanceService.listBySubOrganizationAndDate(subOrgId, date);
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
