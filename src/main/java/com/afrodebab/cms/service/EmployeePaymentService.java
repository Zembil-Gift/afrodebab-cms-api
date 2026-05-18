package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmployeePaymentResponse;
import com.afrodebab.cms.dto.MarkEmployeePaymentPaidRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Admin;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.EmployeePayment;
import com.afrodebab.cms.jpa.repository.AdminRepository;
import com.afrodebab.cms.jpa.repository.EmployeePaymentRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EmployeePaymentService {
    private static final int DUE_WINDOW_DAYS = 3;

    private final EmployeePaymentRepository employeePaymentRepo;
    private final EmployeeRepository employeeRepo;
    private final AdminRepository adminRepo;
    private final EmailNotificationService emailNotificationService;

    public EmployeePaymentService(EmployeePaymentRepository employeePaymentRepo,
                                  EmployeeRepository employeeRepo,
                                  AdminRepository adminRepo,
                                  EmailNotificationService emailNotificationService) {
        this.employeePaymentRepo = employeePaymentRepo;
        this.employeeRepo = employeeRepo;
        this.adminRepo = adminRepo;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public List<EmployeePaymentResponse> getDuePaymentsForAdmin() {
        generatePendingPaymentsForCurrentCycles();
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).plusDays(DUE_WINDOW_DAYS);
        return employeePaymentRepo.findAllByStatusAndDueDateLessThanEqualOrderByDueDateAsc(EmployeePayment.PaymentStatus.PENDING, cutoff)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeePaymentResponse markPaymentAsPaid(Long paymentId, MarkEmployeePaymentPaidRequest req) {
        String normalizedReference = req.transactionReference().trim();
        if (normalizedReference.isEmpty()) {
            throw new BadRequestException("transactionReference is required");
        }
        if (employeePaymentRepo.existsByTransactionReferenceIgnoreCase(normalizedReference)) {
            throw new BadRequestException("transactionReference already exists");
        }

        EmployeePayment payment = employeePaymentRepo.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (payment.getStatus() == EmployeePayment.PaymentStatus.PAID) {
            throw new BadRequestException("Payment is already marked as paid");
        }

        Long paidAmountMinor = req.paidAmountMinor() != null ? req.paidAmountMinor() : payment.getAmountMinor();
        if (paidAmountMinor == null || paidAmountMinor <= 0) {
            throw new BadRequestException("paidAmountMinor must be greater than zero");
        }

        payment.setPaidAmountMinor(paidAmountMinor);
        payment.setTransactionReference(normalizedReference);
        payment.setPaidAt(Instant.now());
        payment.setStatus(EmployeePayment.PaymentStatus.PAID);
        employeePaymentRepo.save(payment);

        Employee employee = payment.getEmployee();
        if (payment.getCycleStartDate() == null) {
            throw new BadRequestException("Employee salaryDate is not set");
        }
        employee.setSalaryEffectiveDate(payment.getCycleStartDate().plusDays(30));
        employeeRepo.save(employee);

        emailNotificationService.queueEmployeePaymentReceivedEmail(
                employee.getEmail(),
                employee.getName(),
                paidAmountMinor,
                normalizedReference,
                payment.getDueDate()
        );

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<EmployeePaymentResponse> getOwnPaymentHistory(String employeeEmail) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return employeePaymentRepo.findAllByEmployeeIdOrderByDueDateDesc(employee.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeePaymentResponse> getOwnPaidPaymentHistory(String employeeEmail) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return employeePaymentRepo.findAllByEmployeeIdAndStatusOrderByDueDateDesc(employee.getId(), EmployeePayment.PaymentStatus.PAID)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeePaymentResponse> getPaidPaymentsForAdmin() {
        return employeePaymentRepo.findAllByStatusOrderByPaidAtDescDueDateDesc(EmployeePayment.PaymentStatus.PAID)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeePaymentResponse> getPaidPaymentsForAdminByYearAndMonth(int year, int month) {
        if (year <= 0) {
            throw new BadRequestException("year must be greater than zero");
        }
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        return employeePaymentRepo.findAllByStatusAndDueDateBetweenOrderByDueDateDesc(
                        EmployeePayment.PaymentStatus.PAID,
                        monthStart,
                        monthEnd
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Scheduled(cron = "0 0 0 */3 * *", zone = "UTC")
    @Transactional
    public void runPaymentReminderCron() {
        generatePendingPaymentsForCurrentCycles();
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).plusDays(DUE_WINDOW_DAYS);
        List<EmployeePayment> dueUnreminded = employeePaymentRepo
                .findAllByStatusAndDueDateLessThanEqualAndLastReminderSentAtIsNullOrderByDueDateAsc(
                        EmployeePayment.PaymentStatus.PENDING,
                        cutoff
                );

        if (dueUnreminded.isEmpty()) {
            return;
        }

        List<Admin> activeAdmins = adminRepo.findAllByActiveTrue();
        if (activeAdmins.isEmpty()) {
            return;
        }

        for (Admin admin : activeAdmins) {
            emailNotificationService.queueAdminPayrollReminderEmail(admin.getEmail(), admin.getName(), dueUnreminded.size());
        }

        Instant now = Instant.now();
        dueUnreminded.forEach(payment -> payment.setLastReminderSentAt(now));
        employeePaymentRepo.saveAll(dueUnreminded);
    }

    private void generatePendingPaymentsForCurrentCycles() {
        List<Employee> candidates = employeeRepo.findAllByActiveTrueAndSalaryEffectiveDateIsNotNullAndSalaryAmountMinorIsNotNull();
        Set<Long> processedEmployeeIds = new HashSet<>();

        for (Employee employee : candidates) {
            if (!processedEmployeeIds.add(employee.getId())) {
                continue;
            }

            LocalDate cycleStart = employee.getSalaryEffectiveDate();
            if (cycleStart == null) {
                continue;
            }

            Long amountMinor = employee.getSalaryAmountMinor();
            if (amountMinor == null) {
                continue;
            }

            employeePaymentRepo.insertPendingIfAbsent(
                    employee.getId(),
                    cycleStart,
                    cycleStart.plusDays(30),
                    amountMinor,
                    EmployeePayment.PaymentStatus.PENDING.name()
            );
        }
    }

    private EmployeePaymentResponse toResponse(EmployeePayment payment) {
        return new EmployeePaymentResponse(
                payment.getId(),
                payment.getEmployee().getId(),
                payment.getEmployee().getName(),
                payment.getCycleStartDate(),
                payment.getDueDate(),
                payment.getAmountMinor(),
                payment.getPaidAmountMinor(),
                payment.getStatus().name(),
                payment.getTransactionReference(),
                payment.getPaidAt(),
                payment.getLastReminderSentAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
