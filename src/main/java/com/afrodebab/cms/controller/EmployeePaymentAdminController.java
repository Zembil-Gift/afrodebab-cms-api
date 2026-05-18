package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeePaymentResponse;
import com.afrodebab.cms.dto.MarkEmployeePaymentPaidRequest;
import com.afrodebab.cms.service.EmployeePaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Employee Payments")
@RestController
@RequestMapping("/admin/payments")
public class EmployeePaymentAdminController {
    private final EmployeePaymentService employeePaymentService;

    public EmployeePaymentAdminController(EmployeePaymentService employeePaymentService) {
        this.employeePaymentService = employeePaymentService;
    }

    @GetMapping("/due")
    public List<EmployeePaymentResponse> duePayments() {
        return employeePaymentService.getDuePaymentsForAdmin();
    }

    @GetMapping("/paid")
    public List<EmployeePaymentResponse> paidPayments() {
        return employeePaymentService.getPaidPaymentsForAdmin();
    }

    @GetMapping("/paid/filter")
    public List<EmployeePaymentResponse> paidPaymentsByYearAndMonth(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return employeePaymentService.getPaidPaymentsForAdminByYearAndMonth(year, month);
    }

    @PostMapping("/{paymentId}/mark-paid")
    public EmployeePaymentResponse markPaid(
            @PathVariable Long paymentId,
            @Valid @RequestBody MarkEmployeePaymentPaidRequest req
    ) {
        return employeePaymentService.markPaymentAsPaid(paymentId, req);
    }
}
