package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.JobApplicationAnswer;

import java.time.Instant;
import java.util.List;

/** What an interviewer sees when they open their feedback link. */
public record InterviewFeedbackFormResponse(
        String organizationName,
        String organizationLogoUrl,
        String interviewerName,
        Instant expiresAt,
        Candidate candidate,
        InterviewDetails interview
) {
    public record Candidate(String fullName, String email, String phoneNumber, String githubUrl, String resumeUrl,
                            List<JobApplicationAnswer> answers, String aiOverview) {}

    public record InterviewDetails(String jobTitle, Instant startAt, Instant endAt, String timezone, String mode,
                                   String location, String meetingUrl, String notes, String status,
                                   List<String> panel) {}
}
