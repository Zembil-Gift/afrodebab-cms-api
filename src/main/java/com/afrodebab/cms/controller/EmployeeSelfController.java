package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeeAttendanceResponse;
import com.afrodebab.cms.dto.EmployeeChangePasswordRequest;
import com.afrodebab.cms.dto.EmployeePaymentResponse;
import com.afrodebab.cms.dto.EmployeeResponse;
import com.afrodebab.cms.service.EmployeeAttendanceService;
import com.afrodebab.cms.service.EmployeePaymentService;
import com.afrodebab.cms.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Employee - Self")
@RestController
@RequestMapping("/employee/me")
public class EmployeeSelfController {
    private final EmployeeService service;
    private final EmployeeAttendanceService attendanceService;
    private final EmployeePaymentService employeePaymentService;

    public EmployeeSelfController(EmployeeService service,
                                  EmployeeAttendanceService attendanceService,
                                  EmployeePaymentService employeePaymentService) {
        this.service = service;
        this.attendanceService = attendanceService;
        this.employeePaymentService = employeePaymentService;
    }

    @PostMapping("/password")
    public void changePassword(Authentication authentication, @Valid @RequestBody EmployeeChangePasswordRequest req) {
        service.changeOwnPassword(authentication.getName(), req);
    }

    @GetMapping
    public EmployeeResponse me(Authentication authentication) {
        return service.getOwnProfile(authentication.getName());
    }

    @PostMapping("/photo")
    public EmployeeResponse uploadPhoto(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return service.uploadOwnPhoto(authentication.getName(), file);
    }

    @PostMapping("/clock-in")
    public EmployeeAttendanceResponse clockIn(Authentication authentication) {
        return attendanceService.clockIn(authentication.getName());
    }

    @PostMapping("/clock-out")
    public EmployeeAttendanceResponse clockOut(Authentication authentication) {
        return attendanceService.clockOut(authentication.getName());
    }

    @PostMapping("/lunch-break-in")
    public EmployeeAttendanceResponse lunchBreakIn(Authentication authentication) {
        return attendanceService.lunchBreakIn(authentication.getName());
    }

    @PostMapping("/lunch-break-out")
    public EmployeeAttendanceResponse lunchBreakOut(Authentication authentication) {
        return attendanceService.lunchBreakOut(authentication.getName());
    }

    @GetMapping("/payments")
    public List<EmployeePaymentResponse> payments(Authentication authentication) {
        return employeePaymentService.getOwnPaymentHistory(authentication.getName());
    }

    @GetMapping("/payments/paid")
    public List<EmployeePaymentResponse> paidPayments(Authentication authentication) {
        return employeePaymentService.getOwnPaidPaymentHistory(authentication.getName());
    }
}
