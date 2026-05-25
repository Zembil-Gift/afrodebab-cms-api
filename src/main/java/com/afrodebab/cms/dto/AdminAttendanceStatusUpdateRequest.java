package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.EmployeeAttendance;
import jakarta.validation.constraints.NotNull;

public record AdminAttendanceStatusUpdateRequest(
        @NotNull(message = "finalStatus is required") EmployeeAttendance.AttendanceFinalStatus finalStatus
) {
}
