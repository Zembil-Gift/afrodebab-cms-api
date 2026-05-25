package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmployeeTimeSpentResponse;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.EmployeeAttendance;
import com.afrodebab.cms.jpa.repository.EmployeeAttendanceRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class EmployeeTimeSpentService {
    private static final long REQUIRED_MINUTES_PER_DAY = 8L * 60L;

    private final EmployeeRepository employeeRepository;
    private final EmployeeAttendanceRepository employeeAttendanceRepository;

    public EmployeeTimeSpentService(EmployeeRepository employeeRepository,
                                    EmployeeAttendanceRepository employeeAttendanceRepository) {
        this.employeeRepository = employeeRepository;
        this.employeeAttendanceRepository = employeeAttendanceRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeTimeSpentResponse getOwnDaily(String employeeEmail, LocalDate date) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return buildDaily(employee, date);
    }

    @Transactional(readOnly = true)
    public EmployeeTimeSpentResponse getOwnWeekly(String employeeEmail, LocalDate referenceDate) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return buildWeekly(employee, referenceDate);
    }

    @Transactional(readOnly = true)
    public EmployeeTimeSpentResponse getOwnMonthly(String employeeEmail, LocalDate referenceDate) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return buildMonthly(employee, referenceDate);
    }

    @Transactional(readOnly = true)
    public EmployeeTimeSpentResponse getEmployeeDaily(Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return buildDaily(employee, date);
    }

    @Transactional(readOnly = true)
    public EmployeeTimeSpentResponse getEmployeeWeekly(Long employeeId, LocalDate referenceDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return buildWeekly(employee, referenceDate);
    }

    @Transactional(readOnly = true)
    public EmployeeTimeSpentResponse getEmployeeMonthly(Long employeeId, LocalDate referenceDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return buildMonthly(employee, referenceDate);
    }

    private EmployeeTimeSpentResponse buildDaily(Employee employee, LocalDate date) {
        List<EmployeeAttendance> rows = employeeAttendanceRepository
                .findAllByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employee.getId(), date, date);
        long workedMinutes = rows.isEmpty() ? 0L : calculateWorkedMinutes(rows.get(0));
        long requiredMinutes = REQUIRED_MINUTES_PER_DAY;
        long remainingMinutes = Math.max(requiredMinutes - workedMinutes, 0L);
        BigDecimal completionPercent = toPercent(workedMinutes, requiredMinutes);

        return new EmployeeTimeSpentResponse(
                employee.getId(),
                employee.getName(),
                "DAILY",
                date,
                date,
                1,
                workedMinutes,
                requiredMinutes,
                remainingMinutes,
                completionPercent
        );
    }

    private EmployeeTimeSpentResponse buildWeekly(Employee employee, LocalDate referenceDate) {
        LocalDate periodStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate periodEnd = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return buildOfficeDayBasedPeriod(employee, "WEEKLY", periodStart, periodEnd);
    }

    private EmployeeTimeSpentResponse buildMonthly(Employee employee, LocalDate referenceDate) {
        YearMonth yearMonth = YearMonth.from(referenceDate);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        return buildOfficeDayBasedPeriod(employee, "MONTHLY", periodStart, periodEnd);
    }

    private EmployeeTimeSpentResponse buildOfficeDayBasedPeriod(Employee employee,
                                                                String periodType,
                                                                LocalDate periodStart,
                                                                LocalDate periodEnd) {
        Set<DayOfWeek> officeDays = employee.getOfficeDays();
        boolean hasConfiguredSchedule = officeDays != null && !officeDays.isEmpty();

        Map<LocalDate, EmployeeAttendance> byDate = employeeAttendanceRepository
                .findAllByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employee.getId(), periodStart, periodEnd)
                .stream()
                .collect(Collectors.toMap(EmployeeAttendance::getAttendanceDate, a -> a, (first, second) -> second));

        List<LocalDate> includedDates;
        if (hasConfiguredSchedule) {
            includedDates = periodStart.datesUntil(periodEnd.plusDays(1))
                    .filter(date -> officeDays.contains(date.getDayOfWeek()))
                    .toList();
        } else {
            includedDates = byDate.keySet().stream()
                    .sorted()
                    .toList();
        }

        Predicate<EmployeeAttendance> approvedLeave = attendance -> {
            if (attendance == null || attendance.getAttendanceStatus() == null) {
                return false;
            }
            String finalStatus = attendance.getAttendanceStatus().get("final");
            return "APPROVED_LEAVE".equals(finalStatus);
        };

        int officeDaysCount = 0;
        long workedMinutes = 0L;
        for (LocalDate date : includedDates) {
            EmployeeAttendance attendance = byDate.get(date);
            if (approvedLeave.test(attendance)) {
                continue;
            }
            officeDaysCount++;
            if (attendance != null) {
                workedMinutes += calculateWorkedMinutes(attendance);
            }
        }

        long requiredMinutes = officeDaysCount * REQUIRED_MINUTES_PER_DAY;
        long remainingMinutes = Math.max(requiredMinutes - workedMinutes, 0L);
        BigDecimal completionPercent = toPercent(workedMinutes, requiredMinutes);

        return new EmployeeTimeSpentResponse(
                employee.getId(),
                employee.getName(),
                periodType,
                periodStart,
                periodEnd,
                officeDaysCount,
                workedMinutes,
                requiredMinutes,
                remainingMinutes,
                completionPercent
        );
    }

    private long calculateWorkedMinutes(EmployeeAttendance attendance) {
        if (attendance.getClockInAt() != null
                && attendance.getClockOutAt() != null
                && attendance.getLunchBreakInAt() != null
                && attendance.getLunchBreakOutAt() != null) {
            long totalMinutes = Duration.between(attendance.getClockInAt(), attendance.getClockOutAt()).toMinutes();
            long lunchMinutes = Duration.between(attendance.getLunchBreakInAt(), attendance.getLunchBreakOutAt()).toMinutes();
            long worked = totalMinutes - lunchMinutes;
            return Math.max(worked, 0L);
        }

        return 0L;
    }

    private BigDecimal toPercent(long workedMinutes, long requiredMinutes) {
        if (requiredMinutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(workedMinutes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requiredMinutes), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
