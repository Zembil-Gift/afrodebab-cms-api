package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeePaymentResponse;
import com.afrodebab.cms.dto.MarkEmployeePaymentPaidRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.service.EmployeePaymentService;
import com.afrodebab.cms.tenant.SubOrgContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Payroll for a vice manager's own branch; the branch always comes from their JWT. */
@Tag(name = "Vice Manager - Payroll")
@RestController
@RequestMapping("/vice-manager/payments")
public class ViceManagerPaymentController {

    private final EmployeePaymentService employeePaymentService;

    public ViceManagerPaymentController(EmployeePaymentService employeePaymentService) {
        this.employeePaymentService = employeePaymentService;
    }

    @GetMapping("/due")
    public List<EmployeePaymentResponse> duePayments() {
        return employeePaymentService.getDuePaymentsForAdmin(subOrgId());
    }

    @GetMapping("/paid")
    public List<EmployeePaymentResponse> paidPayments() {
        return employeePaymentService.getPaidPaymentsForAdmin(subOrgId());
    }

    @GetMapping("/paid/filter")
    public List<EmployeePaymentResponse> paidPaymentsByYearAndMonth(@RequestParam int year, @RequestParam int month) {
        return employeePaymentService.getPaidPaymentsForAdminByYearAndMonth(year, month, subOrgId());
    }

    @PostMapping("/{paymentId}/mark-paid")
    public EmployeePaymentResponse markPaid(@PathVariable Long paymentId,
                                            @Valid @RequestBody MarkEmployeePaymentPaidRequest req) {
        return employeePaymentService.markPaymentAsPaid(paymentId, req, subOrgId());
    }

    private static Long subOrgId() {
        Long subOrgId = SubOrgContext.get();
        if (subOrgId == null) {
            throw new BadRequestException("No sub-organization assigned to this Vice Manager");
        }
        return subOrgId;
    }
}
