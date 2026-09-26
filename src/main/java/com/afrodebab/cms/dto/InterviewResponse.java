package com.afrodebab.cms.dto;

import java.time.Instant;
import java.util.List;

public record InterviewResponse(
        Long id,
        Long applicationId,
        String candidateName,
        String candidateEmail,
        String jobTitle,
        Instant startAt,
        Instant endAt,
        String mode,
        String location,
        String meetingUrl,
        String notes,
        String status,
        boolean googleCalendarEvent,
        List<Participant> participants,
        Instant createdAt
) {
    public record Participant(String kind, Long managerId, Long employeeId, String name, String email) {}
}
