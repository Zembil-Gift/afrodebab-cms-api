package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeeAttendanceEmailRequest;
import com.afrodebab.cms.dto.EmployeeAttendanceResponse;
import com.afrodebab.cms.dto.EmployeeChangePasswordRequest;
import com.afrodebab.cms.dto.EmployeeConnectedAccountsResponse;
import com.afrodebab.cms.dto.EmployeeConnectedAccountsUpdateRequest;
import com.afrodebab.cms.dto.EmployeePaymentResponse;
import com.afrodebab.cms.dto.EmployeeResponse;
import com.afrodebab.cms.security.AttendanceKeyValidator;
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
    private final AttendanceKeyValidator attendanceKeyValidator;

    public EmployeeSelfController(EmployeeService service,
                                  EmployeeAttendanceService attendanceService,
                                  EmployeePaymentService employeePaymentService,
                                  AttendanceKeyValidator attendanceKeyValidator) {
        this.service = service;
        this.attendanceService = attendanceService;
        this.employeePaymentService = employeePaymentService;
        this.attendanceKeyValidator = attendanceKeyValidator;
    }

    @PostMapping("/password")
    public void changePassword(Authentication authentication, @Valid @RequestBody EmployeeChangePasswordRequest req) {
        service.changeOwnPassword(authentication.getName(), req);
    }

    @GetMapping
    public EmployeeResponse me(Authentication authentication) {
        return service.getOwnProfile(authentication.getName());
    }




    @GetMapping("/connected-accounts")
    public EmployeeConnectedAccountsResponse connectedAccounts(Authentication authentication) {
        return service.getOwnConnectedAccounts(authentication.getName());
    }

    @PatchMapping("/connected-accounts")
    public EmployeeConnectedAccountsResponse updateConnectedAccounts(Authentication authentication,
                                                                     @RequestBody EmployeeConnectedAccountsUpdateRequest req) {
        return service.updateOwnConnectedAccounts(authentication.getName(), req);
    }

    @PostMapping("/photo")
    public EmployeeResponse uploadPhoto(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return service.uploadOwnPhoto(authentication.getName(), file);
    }

    @RequestMapping(value = "/profile", method = {RequestMethod.PATCH, RequestMethod.POST}, consumes = "multipart/form-data")
    public EmployeeResponse updateProfile(Authentication authentication,
                                          @RequestParam(required = false) String linkedinUrl,
                                          @RequestParam(name = "photo", required = false) MultipartFile photo,
                                          @RequestParam(name = "file", required = false) MultipartFile file) {
        MultipartFile upload = (photo != null && !photo.isEmpty()) ? photo : file;
        return service.updateOwnProfile(authentication.getName(), linkedinUrl, upload);
    }

    @PostMapping("/clock-in")
    public EmployeeAttendanceResponse clockIn(
            @RequestHeader(value = "X-Employee-Attendance-Key", required = false) String attendanceKey,
            @Valid @RequestBody EmployeeAttendanceEmailRequest req
    ) {
        attendanceKeyValidator.requireValidKey(attendanceKey);
        return attendanceService.clockIn(req.email());
    }

    @PostMapping("/clock-out")
    public EmployeeAttendanceResponse clockOut(
            @RequestHeader(value = "X-Employee-Attendance-Key", required = false) String attendanceKey,
            @Valid @RequestBody EmployeeAttendanceEmailRequest req
    ) {
        attendanceKeyValidator.requireValidKey(attendanceKey);
        return attendanceService.clockOut(req.email());
    }

    @PostMapping("/lunch-break-in")
    public EmployeeAttendanceResponse lunchBreakIn(
            @RequestHeader(value = "X-Employee-Attendance-Key", required = false) String attendanceKey,
            @Valid @RequestBody EmployeeAttendanceEmailRequest req
    ) {
        attendanceKeyValidator.requireValidKey(attendanceKey);
        return attendanceService.lunchBreakIn(req.email());
    }

    @PostMapping("/lunch-break-out")
    public EmployeeAttendanceResponse lunchBreakOut(
            @RequestHeader(value = "X-Employee-Attendance-Key", required = false) String attendanceKey,
            @Valid @RequestBody EmployeeAttendanceEmailRequest req
    ) {
        attendanceKeyValidator.requireValidKey(attendanceKey);
        return attendanceService.lunchBreakOut(req.email());
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
