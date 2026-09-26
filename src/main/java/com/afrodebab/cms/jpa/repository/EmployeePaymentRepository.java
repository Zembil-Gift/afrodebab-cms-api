package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.EmployeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {
    boolean existsByEmployeeIdAndCycleStartDate(Long employeeId, LocalDate cycleStartDate);
    boolean existsByTransactionReferenceIgnoreCase(String transactionReference);
    List<EmployeePayment> findAllByStatusAndDueDateLessThanEqualOrderByDueDateAsc(
            EmployeePayment.PaymentStatus status,
            LocalDate dueDate
    );
    List<EmployeePayment> findAllByStatusAndDueDateLessThanEqualAndLastReminderSentAtIsNullOrderByDueDateAsc(
            EmployeePayment.PaymentStatus status,
            LocalDate dueDate
    );
    List<EmployeePayment> findAllByEmployeeIdOrderByDueDateDesc(Long employeeId);
    List<EmployeePayment> findAllByEmployeeIdAndStatusOrderByDueDateDesc(Long employeeId, EmployeePayment.PaymentStatus status);
    List<EmployeePayment> findAllByStatusOrderByPaidAtDescDueDateDesc(EmployeePayment.PaymentStatus status);
    List<EmployeePayment> findAllByStatusAndDueDateBetweenOrderByDueDateDesc(
            EmployeePayment.PaymentStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    @Modifying
    @Query(
            value = """
                    INSERT INTO employee_payments (
                        organization_id, employee_id, cycle_start_date, due_date, amount_minor,
                        gross_amount_minor, income_tax_minor, employee_pension_minor, employer_pension_minor,
                        status, created_at, updated_at
                    ) VALUES (
                        :organizationId, :employeeId, :cycleStartDate, :dueDate, :amountMinor,
                        :grossAmountMinor, :incomeTaxMinor, :employeePensionMinor, :employerPensionMinor,
                        :status, NOW(), NOW()
                    )
                    ON CONFLICT (employee_id, cycle_start_date) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertPendingIfAbsent(@Param("organizationId") Long organizationId,
                              @Param("employeeId") Long employeeId,
                              @Param("cycleStartDate") LocalDate cycleStartDate,
                              @Param("dueDate") LocalDate dueDate,
                              @Param("amountMinor") Long amountMinor,
                              @Param("grossAmountMinor") Long grossAmountMinor,
                              @Param("incomeTaxMinor") Long incomeTaxMinor,
                              @Param("employeePensionMinor") Long employeePensionMinor,
                              @Param("employerPensionMinor") Long employerPensionMinor,
                              @Param("status") String status);
}
