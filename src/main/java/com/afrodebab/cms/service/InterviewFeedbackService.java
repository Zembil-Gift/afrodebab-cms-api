package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.InterviewFeedbackFormResponse;
import com.afrodebab.cms.dto.InterviewFeedbackSubmitRequest;
import com.afrodebab.cms.dto.InterviewResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Interview;
import com.afrodebab.cms.jpa.entity.InterviewFeedback;
import com.afrodebab.cms.jpa.entity.InterviewParticipant;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.InterviewFeedbackRepository;
import com.afrodebab.cms.jpa.repository.InterviewRepository;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * No-login feedback links for interviewers. Each interviewer gets their own link; it shows the
 * candidate and interview details and works until they submit their feedback or it expires.
 * Only the token's SHA-256 hash is stored, so a database leak doesn't hand out working links.
 * Once every interviewer has submitted, the interview is marked COMPLETED.
 */
@Service
public class InterviewFeedbackService {
    private static final Logger log = LoggerFactory.getLogger(InterviewFeedbackService.class);
    private static final Duration LINK_LIFETIME_AFTER_INTERVIEW = Duration.ofDays(14);
    private static final String INVALID_LINK = "This feedback link is invalid or has already been used";

    private final InterviewFeedbackRepository feedbackRepo;
    private final InterviewRepository interviewRepo;
    private final OrganizationRepository organizationRepo;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public InterviewFeedbackService(InterviewFeedbackRepository feedbackRepo,
                                    InterviewRepository interviewRepo,
                                    OrganizationRepository organizationRepo,
                                    @Value("${app.frontend-url:}") String frontendUrl) {
        this.feedbackRepo = feedbackRepo;
        this.interviewRepo = interviewRepo;
        this.organizationRepo = organizationRepo;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    // ---------------------------------------------------------------- manager side (called by InterviewService)

    /**
     * Issues a fresh link to every interviewer who hasn't submitted yet (earlier links stop working)
     * and drops pending links of people no longer on the panel. Returns email → feedback URL.
     */
    @Transactional
    public Map<String, String> issueLinks(Interview interview) {
        if (frontendUrl.isEmpty()) {
            log.warn("FRONTEND_URL is not set; interview feedback links are not sent");
            return Map.of();
        }
        Map<String, InterviewFeedback> existing = feedbackRepo.findAllByInterviewIdOrderByIdAsc(interview.getId()).stream()
                .collect(Collectors.toMap(InterviewFeedback::getInterviewerEmail, Function.identity()));
        Set<String> panel = interview.getParticipants().stream().map(InterviewParticipant::getEmail).collect(Collectors.toSet());
        feedbackRepo.deleteAll(existing.values().stream()
                .filter(f -> f.getSubmittedAt() == null && !panel.contains(f.getInterviewerEmail()))
                .toList());

        Map<String, String> urls = new HashMap<>();
        for (InterviewParticipant p : interview.getParticipants()) {
            InterviewFeedback feedback = existing.getOrDefault(p.getEmail(), new InterviewFeedback());
            if (feedback.getSubmittedAt() != null) continue;
            String token = newToken();
            feedback.setInterview(interview);
            feedback.setInterviewerName(p.getName());
            feedback.setInterviewerEmail(p.getEmail());
            feedback.setTokenHash(hash(token));
            feedback.setExpiresAt(interview.getEndAt().plus(LINK_LIFETIME_AFTER_INTERVIEW));
            feedbackRepo.save(feedback);
            urls.put(p.getEmail(), frontendUrl + "/interview-feedback/" + token);
        }
        return urls;
    }

    /** Kills every unused link of the interview (cancelled or no-show). Submitted feedback is kept. */
    @Transactional
    public void revokeLinks(Interview interview) {
        feedbackRepo.deleteAll(feedbackRepo.findAllByInterviewIdOrderByIdAsc(interview.getId()).stream()
                .filter(f -> f.getSubmittedAt() == null)
                .toList());
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse.Feedback> feedbackFor(Long interviewId) {
        return feedbackRepo.findAllByInterviewIdOrderByIdAsc(interviewId).stream()
                .map(f -> new InterviewResponse.Feedback(f.getInterviewerName(), f.getInterviewerEmail(),
                        f.getRecommendation() == null ? null : f.getRecommendation().name(), f.getComments(),
                        f.getSubmittedAt()))
                .toList();
    }

    // ---------------------------------------------------------------- interviewer side (anonymous link)

    /** The org that owns the link, so the caller can scope the request to it. */
    public Long resolveOrganizationId(String token) {
        return feedbackRepo.findOrganizationIdByTokenHash(hash(token))
                .orElseThrow(() -> new NotFoundException(INVALID_LINK));
    }

    @Transactional(readOnly = true)
    public InterviewFeedbackFormResponse getForm(String token) {
        return toFormResponse(getUsableOrThrow(feedbackRepo.findByTokenHash(hash(token)).orElse(null)));
    }

    @Transactional
    public void submit(String token, InterviewFeedbackSubmitRequest req) {
        // Row locks stop a double submit of one link and let exactly one submitter see "everyone is done".
        InterviewFeedback feedback = getUsableOrThrow(feedbackRepo.findForUpdateByTokenHash(hash(token)).orElse(null));
        Interview interview = interviewRepo.findForUpdateById(feedback.getInterview().getId())
                .orElseThrow(() -> new NotFoundException(INVALID_LINK));
        if (Instant.now().isBefore(interview.getStartAt())) {
            throw new BadRequestException("Feedback opens when the interview starts");
        }

        feedback.setRecommendation(req.recommendation());
        feedback.setComments(req.comments().trim());
        feedback.setSubmittedAt(Instant.now());
        feedback.setTokenHash(null);
        feedbackRepo.saveAndFlush(feedback);

        boolean allSubmitted = feedbackRepo.findAllByInterviewIdOrderByIdAsc(interview.getId()).stream()
                .allMatch(f -> f.getSubmittedAt() != null);
        if (allSubmitted && interview.getStatus() == Interview.Status.SCHEDULED) {
            interview.setStatus(Interview.Status.COMPLETED);
            interviewRepo.save(interview);
        }
    }

    // ---------------------------------------------------------------- helpers

    private InterviewFeedback getUsableOrThrow(InterviewFeedback feedback) {
        if (feedback == null) throw new NotFoundException(INVALID_LINK);
        if (Instant.now().isAfter(feedback.getExpiresAt())) throw new NotFoundException("This feedback link has expired");
        return feedback;
    }

    private InterviewFeedbackFormResponse toFormResponse(InterviewFeedback feedback) {
        Interview i = feedback.getInterview();
        JobApplication app = i.getApplication();
        Organization org = organizationRepo.findById(i.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        boolean hasAiOverview = app.getAiOverviewStatus() == JobApplication.AiOverviewStatus.COMPLETED;
        return new InterviewFeedbackFormResponse(
                org.getName(),
                org.getLogoUrl(),
                feedback.getInterviewerName(),
                feedback.getExpiresAt(),
                new InterviewFeedbackFormResponse.Candidate(app.getFullName(), app.getEmail(), app.getPhoneNumber(),
                        app.getGithubUrl(), app.getResumeUrl(), app.getAnswers(),
                        hasAiOverview ? app.getAiOverviewText() : null),
                new InterviewFeedbackFormResponse.InterviewDetails(app.getJob().getTitle(), i.getStartAt(), i.getEndAt(),
                        org.getEmailTimezone(), i.getMode().name(), i.getLocation(), i.getMeetingUrl(), i.getNotes(),
                        i.getStatus().name(),
                        i.getParticipants().stream()
                                .map(p -> p.getName() == null ? p.getEmail() : p.getName())
                                .toList()));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
