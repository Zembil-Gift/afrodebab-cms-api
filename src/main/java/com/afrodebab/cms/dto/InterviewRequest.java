package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.Interview;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Schedule or reschedule an interview. Interviewers come from three lists: org managers,
 * employees, and outside people by email. For ONLINE interviews without a meeting URL, a Google
 * Meet link is created when the scheduling manager has connected Google Calendar.
 */
public record InterviewRequest(
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @NotNull Interview.Mode mode,
        @Size(max = 500) String location,
        @Size(max = 1000) String meetingUrl,
        @Size(max = 5000) String notes,
        List<Long> managerIds,
        List<Long> employeeIds,
        @Valid List<ExternalInvitee> externalInvitees
) {
    public record ExternalInvitee(@Size(max = 255) String name, @NotBlank @Email @Size(max = 255) String email) {}
}
