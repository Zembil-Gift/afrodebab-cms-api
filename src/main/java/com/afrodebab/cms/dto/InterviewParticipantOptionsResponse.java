package com.afrodebab.cms.dto;

import java.util.List;

/** Who can be picked as an interviewer (active managers and employees of the org). */
public record InterviewParticipantOptionsResponse(List<Option> managers, List<Option> employees) {
    public record Option(Long id, String name, String email, String detail) {}
}
