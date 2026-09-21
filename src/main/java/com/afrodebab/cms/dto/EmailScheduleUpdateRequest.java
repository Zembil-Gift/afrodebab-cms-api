package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** dispatchTime is local to timezone (IANA id, e.g. "Africa/Addis_Ababa"). */
public record EmailScheduleUpdateRequest(
        @NotBlank(message = "dispatchTime is required")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "dispatchTime must be HH:mm (24-hour)")
        String dispatchTime,
        @NotBlank(message = "timezone is required") String timezone,
        @NotNull(message = "payrollReminderIntervalDays is required")
        @Min(value = 1, message = "payrollReminderIntervalDays must be between 1 and 30")
        @Max(value = 30, message = "payrollReminderIntervalDays must be between 1 and 30")
        Integer payrollReminderIntervalDays
) {}
