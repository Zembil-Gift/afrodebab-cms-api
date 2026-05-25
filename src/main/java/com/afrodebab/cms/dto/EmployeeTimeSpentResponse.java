package com.afrodebab.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeTimeSpentResponse(
        Long employeeId,
        String employeeName,
        String periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        int officeDaysCount,
        long workedMinutes,
        long requiredMinutes,
        long remainingMinutes,
        BigDecimal completionPercent
) {
}
