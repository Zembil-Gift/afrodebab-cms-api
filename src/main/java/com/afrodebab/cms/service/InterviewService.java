package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.InterviewParticipantOptionsResponse;
import com.afrodebab.cms.dto.InterviewRequest;
import com.afrodebab.cms.dto.InterviewResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Interview;
import com.afrodebab.cms.jpa.entity.InterviewParticipant;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.Notification;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.InterviewRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import com.afrodebab.cms.util.ICalendar;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Interviews for job applications. When the scheduling manager has connected Google Calendar,
 * the interview becomes an event in their calendar (Google sends the invitations and, for
 * online interviews, creates a Meet link). Otherwise we email each person a branded
 * invitation with an .ics attachment. Internal interviewers also get an in-app notification.
 * Internal notes never reach the candidate: they are left out of the Google event and the
 * candidate's email.
 */
@Service
public class InterviewService {
    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);
    private static final String CALENDAR_EVENTS = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final InterviewRepository interviewRepo;
    private final JobApplicationService jobApplicationService;
    private final ManagerRepository managerRepo;
    private final EmployeeRepository employeeRepo;
    private final OrganizationRepository organizationRepo;
    private final ManagerGoogleConnectionService googleConnection;
    private final GoogleApiClient google;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    public InterviewService(InterviewRepository interviewRepo,
                            JobApplicationService jobApplicationService,
                            ManagerRepository managerRepo,
                            EmployeeRepository employeeRepo,
                            OrganizationRepository organizationRepo,
                            ManagerGoogleConnectionService googleConnection,
                            GoogleApiClient google,
                            EmailNotificationService emailNotificationService,
                            NotificationService notificationService) {
        this.interviewRepo = interviewRepo;
        this.jobApplicationService = jobApplicationService;
        this.managerRepo = managerRepo;
        this.employeeRepo = employeeRepo;
        this.organizationRepo = organizationRepo;
        this.googleConnection = googleConnection;
        this.google = google;
        this.emailNotificationService = emailNotificationService;
        this.notificationService = notificationService;
    }

    // ---------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public List<InterviewResponse> listForApplication(Long applicationId) {
        jobApplicationService.getEntityOrThrow(applicationId);
        return interviewRepo.findAllByApplicationIdOrderByStartAtDesc(applicationId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> listForJob(Long jobId) {
        return interviewRepo.findAllByApplicationJobIdOrderByStartAtAsc(jobId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InterviewParticipantOptionsResponse participantOptions() {
        var managers = managerRepo.findAllByActiveTrue().stream()
                .sorted(Comparator.comparing(Manager::getName, String.CASE_INSENSITIVE_ORDER))
                .map(m -> new InterviewParticipantOptionsResponse.Option(m.getId(), m.getName(), m.getEmail(),
                        m.getRole() == Manager.ManagerRole.VICE_MANAGER ? "Vice manager" : "Manager"))
                .toList();
        var employees = employeeRepo.findAllByActiveTrueOrderByNameAsc().stream()
                .map(e -> new InterviewParticipantOptionsResponse.Option(e.getId(), e.getName(), e.getEmail(), e.getPosition()))
                .toList();
        return new InterviewParticipantOptionsResponse(managers, employees);
    }

    // ---------------------------------------------------------------- commands

    @Transactional
    public InterviewResponse schedule(Long applicationId, InterviewRequest req) {
        JobApplication application = jobApplicationService.getEntityOrThrow(applicationId);
        if (application.getStatus() == JobApplication.ApplicationStatus.REJECTED
                || application.getStatus() == JobApplication.ApplicationStatus.HIRED) {
            throw new BadRequestException("Interviews can't be scheduled for rejected or hired candidates");
        }
        Manager organizer = currentManager();

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setScheduledByManagerId(organizer.getId());
        interview.setStatus(Interview.Status.SCHEDULED);
        apply(interview, req);
        interview = interviewRepo.save(interview);

        if (application.getStatus() != JobApplication.ApplicationStatus.SELECTED_FOR_INTERVIEW) {
            application.setStatus(JobApplication.ApplicationStatus.SELECTED_FOR_INTERVIEW);
        }

        boolean onGoogle = createGoogleEvent(interview, organizer);
        if (!onGoogle) emailInvitations(interview, organizer, interview.getParticipants());
        notifyInternal(interview, interview.getParticipants(), Notification.Type.INTERVIEW_INVITATION);
        return toResponse(interviewRepo.save(interview));
    }

    @Transactional
    public InterviewResponse reschedule(Long interviewId, InterviewRequest req) {
        Interview interview = getScheduledOrThrow(interviewId);
        Manager organizer = organizerOf(interview);
        List<InterviewParticipant> previous = new ArrayList<>(interview.getParticipants());

        apply(interview, req);
        interview.setSequence(interview.getSequence() + 1);
        // People dropped from the panel get a cancellation instead of the update.
        Set<String> kept = emails(interview.getParticipants());
        List<InterviewParticipant> removed = previous.stream().filter(p -> !kept.contains(p.getEmail())).toList();

        // Interviews first sent by email stay on email, so nobody ends up with two calendar entries.
        boolean onGoogle = interview.getGoogleEventId() != null && updateGoogleEvent(interview, organizer);
        if (!onGoogle) {
            emailInvitations(interview, organizer, interview.getParticipants());
            emailCancellations(interview, organizer, removed, false);
        }
        notifyInternal(interview, interview.getParticipants(), Notification.Type.INTERVIEW_INVITATION);
        notifyInternal(interview, removed, Notification.Type.INTERVIEW_CANCELLED);
        return toResponse(interviewRepo.save(interview));
    }

    @Transactional
    public InterviewResponse cancel(Long interviewId) {
        Interview interview = getScheduledOrThrow(interviewId);
        Manager organizer = organizerOf(interview);
        interview.setStatus(Interview.Status.CANCELLED);
        interview.setSequence(interview.getSequence() + 1);

        if (!deleteGoogleEvent(interview, organizer)) {
            emailCancellations(interview, organizer, interview.getParticipants(), true);
        }
        notifyInternal(interview, interview.getParticipants(), Notification.Type.INTERVIEW_CANCELLED);
        return toResponse(interviewRepo.save(interview));
    }

    @Transactional
    public InterviewResponse setOutcome(Long interviewId, Interview.Status status) {
        if (status != Interview.Status.COMPLETED && status != Interview.Status.NO_SHOW) {
            throw new BadRequestException("Status must be COMPLETED or NO_SHOW; use cancel to cancel an interview");
        }
        Interview interview = getEntityOrThrow(interviewId);
        if (interview.getStatus() == Interview.Status.CANCELLED) {
            throw new BadRequestException("Cancelled interviews can't be marked " + status);
        }
        interview.setStatus(status);
        return toResponse(interviewRepo.save(interview));
    }

    // ---------------------------------------------------------------- request → entity

    private void apply(Interview interview, InterviewRequest req) {
        if (!req.endAt().isAfter(req.startAt())) throw new BadRequestException("The interview must end after it starts");
        interview.setStartAt(req.startAt());
        interview.setEndAt(req.endAt());
        interview.setMode(req.mode());
        interview.setLocation(blankToNull(req.location()));
        interview.setNotes(blankToNull(req.notes()));
        String meetingUrl = blankToNull(req.meetingUrl());
        if (meetingUrl != null && !meetingUrl.matches("(?i)^https?://\\S+$")) {
            throw new BadRequestException("Meeting link must be an http(s) URL");
        }
        // Keep a generated Meet link across reschedules unless the manager typed a different one.
        if (meetingUrl != null || req.mode() == Interview.Mode.IN_PERSON) interview.setMeetingUrl(meetingUrl);
        interview.getParticipants().clear();
        interview.getParticipants().addAll(resolveParticipants(req, interview.getApplication().getEmail()));
    }

    private List<InterviewParticipant> resolveParticipants(InterviewRequest req, String candidateEmail) {
        Map<String, InterviewParticipant> byEmail = new LinkedHashMap<>();
        for (Long id : nullSafe(req.managerIds())) {
            Manager m = managerRepo.findById(id).filter(Manager::isActive)
                    .orElseThrow(() -> new NotFoundException("Manager not found: " + id));
            byEmail.putIfAbsent(normalize(m.getEmail()),
                    new InterviewParticipant(InterviewParticipant.Kind.MANAGER, m.getId(), null, m.getName(), normalize(m.getEmail())));
        }
        for (Long id : nullSafe(req.employeeIds())) {
            Employee e = employeeRepo.findById(id).filter(Employee::isActive)
                    .orElseThrow(() -> new NotFoundException("Employee not found: " + id));
            byEmail.putIfAbsent(normalize(e.getEmail()),
                    new InterviewParticipant(InterviewParticipant.Kind.EMPLOYEE, null, e.getId(), e.getName(), normalize(e.getEmail())));
        }
        for (InterviewRequest.ExternalInvitee x : nullSafe(req.externalInvitees())) {
            String email = normalize(x.email());
            byEmail.putIfAbsent(email, new InterviewParticipant(InterviewParticipant.Kind.EXTERNAL, null, null, blankToNull(x.name()), email));
        }
        byEmail.remove(normalize(candidateEmail));
        return new ArrayList<>(byEmail.values());
    }

    // ---------------------------------------------------------------- Google Calendar

    /** True when the interview now lives in the organizer's Google Calendar (Google sends the invites). */
    private boolean createGoogleEvent(Interview interview, Manager organizer) {
        return withCalendar(organizer, token -> {
            JsonNode event = google.request("POST", CALENDAR_EVENTS + "?sendUpdates=all&conferenceDataVersion=1",
                    token, googleEventBody(interview));
            interview.setGoogleEventId(event.path("id").asText(null));
            if (event.hasNonNull("hangoutLink")) interview.setMeetingUrl(event.path("hangoutLink").asText());
            return interview.getGoogleEventId() != null;
        });
    }

    private boolean updateGoogleEvent(Interview interview, Manager organizer) {
        return withCalendar(organizer, token -> {
            JsonNode event = google.request("PATCH", CALENDAR_EVENTS + "/" + interview.getGoogleEventId()
                    + "?sendUpdates=all&conferenceDataVersion=1", token, googleEventBody(interview));
            if (event.hasNonNull("hangoutLink")) interview.setMeetingUrl(event.path("hangoutLink").asText());
            return true;
        });
    }

    private boolean deleteGoogleEvent(Interview interview, Manager organizer) {
        if (interview.getGoogleEventId() == null) return false;
        return withCalendar(organizer, token -> {
            try {
                google.request("DELETE", CALENDAR_EVENTS + "/" + interview.getGoogleEventId() + "?sendUpdates=all", token, null);
            } catch (ResponseStatusException e) {
                if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e; // already deleted in Google
            }
            return true;
        });
    }

    /** Runs a Calendar call with the organizer's token; any failure falls back to email invitations. */
    private boolean withCalendar(Manager organizer, java.util.function.Function<String, Boolean> call) {
        if (organizer == null) return false;
        try {
            return googleConnection.accessToken(organizer, GoogleApiClient.SCOPE_CALENDAR).map(call).orElse(false);
        } catch (RuntimeException e) {
            log.warn("Google Calendar sync failed for manager {}; sending email invitations instead", organizer.getId(), e);
            return false;
        }
    }

    private Map<String, Object> googleEventBody(Interview interview) {
        JobApplication app = interview.getApplication();
        List<Map<String, Object>> attendees = new ArrayList<>();
        attendees.add(Map.of("email", app.getEmail(), "displayName", app.getFullName()));
        interview.getParticipants().forEach(p -> attendees.add(p.getName() == null
                ? Map.of("email", p.getEmail())
                : Map.of("email", p.getEmail(), "displayName", p.getName())));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", "Interview: " + app.getFullName() + " - " + app.getJob().getTitle());
        // Visible to the candidate, so no internal notes here.
        body.put("description", "Interview for " + app.getJob().getTitle() + " at " + currentOrg().getName() + ".");
        body.put("start", Map.of("dateTime", interview.getStartAt().toString()));
        body.put("end", Map.of("dateTime", interview.getEndAt().toString()));
        body.put("attendees", attendees);
        body.put("location", interview.getMode() == Interview.Mode.ONLINE
                ? (interview.getMeetingUrl() == null ? "" : interview.getMeetingUrl())
                : (interview.getLocation() == null ? "" : interview.getLocation()));
        if (interview.getMode() == Interview.Mode.ONLINE && interview.getMeetingUrl() == null) {
            body.put("conferenceData", Map.of("createRequest", Map.of(
                    "requestId", UUID.randomUUID().toString(),
                    "conferenceSolutionKey", Map.of("type", "hangoutsMeet"))));
        }
        return body;
    }

    // ---------------------------------------------------------------- email fallback

    private void emailInvitations(Interview interview, Manager organizer, List<InterviewParticipant> participants) {
        JobApplication app = interview.getApplication();
        String jobTitle = app.getJob().getTitle();
        String when = when(interview);
        String format = interview.getMode() == Interview.Mode.ONLINE ? "Online" : "In person";

        emailNotificationService.queueInterviewInvitationEmail(app.getEmail(), app.getFullName(), jobTitle, when, format,
                interview.getLocation(), interview.getMeetingUrl(),
                invite(interview, organizer, ICalendar.Method.REQUEST, app.getFullName(), app.getEmail()));
        for (InterviewParticipant p : participants) {
            emailNotificationService.queueInterviewPanelInvitationEmail(p.getEmail(), p.getName(), app.getFullName(), jobTitle,
                    when, format, interview.getLocation(), interview.getMeetingUrl(), interview.getNotes(),
                    invite(interview, organizer, ICalendar.Method.REQUEST, p.getName(), p.getEmail()));
        }
    }

    private void emailCancellations(Interview interview, Manager organizer, List<InterviewParticipant> participants,
                                    boolean includeCandidate) {
        JobApplication app = interview.getApplication();
        String when = when(interview);
        if (includeCandidate) {
            emailNotificationService.queueInterviewCancelledEmail(app.getEmail(), app.getFullName(), app.getJob().getTitle(), when,
                    invite(interview, organizer, ICalendar.Method.CANCEL, app.getFullName(), app.getEmail()));
        }
        for (InterviewParticipant p : participants) {
            emailNotificationService.queueInterviewCancelledEmail(p.getEmail(), p.getName(), app.getJob().getTitle(), when,
                    invite(interview, organizer, ICalendar.Method.CANCEL, p.getName(), p.getEmail()));
        }
    }

    // Each person's .ics lists only themselves, so staff emails aren't shared with the candidate.
    private String invite(Interview interview, Manager organizer, ICalendar.Method method, String name, String email) {
        JobApplication app = interview.getApplication();
        ICalendar.Attendee organizerRef = organizer == null ? null : new ICalendar.Attendee(organizer.getName(), organizer.getEmail());
        String location = interview.getMode() == Interview.Mode.ONLINE ? interview.getMeetingUrl() : interview.getLocation();
        ICalendar.Event event = new ICalendar.Event("interview-" + interview.getId() + "@mahberix",
                interview.getStartAt(), interview.getEndAt(),
                "Interview: " + app.getFullName() + " - " + app.getJob().getTitle(),
                "Interview for " + app.getJob().getTitle() + " at " + currentOrg().getName() + ".",
                location, interview.getMeetingUrl(), interview.getSequence(), organizerRef,
                List.of(new ICalendar.Attendee(name == null ? email : name, email)));
        return ICalendar.write(null, method, List.of(event));
    }

    // ---------------------------------------------------------------- in-app notifications

    private void notifyInternal(Interview interview, List<InterviewParticipant> participants, Notification.Type type) {
        JobApplication app = interview.getApplication();
        boolean cancelled = type == Notification.Type.INTERVIEW_CANCELLED;
        String title = (cancelled ? "Interview cancelled: " : "Interview: ") + app.getFullName() + " - " + app.getJob().getTitle();
        StringBuilder body = new StringBuilder("**When:** ").append(when(interview));
        if (!cancelled) {
            body.append("\n**Format:** ").append(interview.getMode() == Interview.Mode.ONLINE ? "Online" : "In person");
            if (interview.getLocation() != null) body.append("\n**Location:** ").append(interview.getLocation());
            if (interview.getMeetingUrl() != null) body.append("\n**Meeting link:** [Join](").append(interview.getMeetingUrl()).append(")");
            if (interview.getNotes() != null) body.append("\n\n**Notes:** ").append(interview.getNotes());
        }
        List<Notification> notifications = participants.stream()
                .filter(p -> p.getKind() != InterviewParticipant.Kind.EXTERNAL)
                .map(p -> {
                    Notification n = new Notification();
                    n.setEmployeeId(p.getEmployeeId());
                    n.setManagerId(p.getManagerId());
                    n.setType(type);
                    n.setTitle(title);
                    n.setBody(body.toString());
                    n.setLink(p.getManagerId() != null ? "/manager/jobs" : null);
                    return n;
                })
                .toList();
        notificationService.createAll(notifications);
    }

    // ---------------------------------------------------------------- helpers

    private String when(Interview interview) {
        ZoneId zone = ZoneId.of(currentOrg().getEmailTimezone());
        ZonedDateTime start = interview.getStartAt().atZone(zone);
        ZonedDateTime end = interview.getEndAt().atZone(zone);
        String endText = start.toLocalDate().equals(end.toLocalDate()) ? TIME.format(end) : DAY.format(end);
        return DAY.format(start) + " - " + endText + " (" + zone.getId() + ")";
    }

    private Interview getEntityOrThrow(Long id) {
        return interviewRepo.findById(id).orElseThrow(() -> new NotFoundException("Interview not found"));
    }

    private Interview getScheduledOrThrow(Long id) {
        Interview interview = getEntityOrThrow(id);
        if (interview.getStatus() != Interview.Status.SCHEDULED) {
            throw new BadRequestException("Only scheduled interviews can be changed");
        }
        return interview;
    }

    /** The manager whose calendar holds the event; falls back to the caller if they left the org. */
    private Manager organizerOf(Interview interview) {
        return interview.getScheduledByManagerId() == null ? currentManager()
                : managerRepo.findById(interview.getScheduledByManagerId()).orElseGet(this::currentManager);
    }

    private Manager currentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private Organization currentOrg() {
        return organizationRepo.findById(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private static Set<String> emails(List<InterviewParticipant> participants) {
        return participants.stream().map(InterviewParticipant::getEmail).collect(Collectors.toSet());
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private InterviewResponse toResponse(Interview i) {
        JobApplication app = i.getApplication();
        return new InterviewResponse(
                i.getId(),
                app.getId(),
                app.getFullName(),
                app.getEmail(),
                app.getJob().getTitle(),
                i.getStartAt(),
                i.getEndAt(),
                i.getMode().name(),
                i.getLocation(),
                i.getMeetingUrl(),
                i.getNotes(),
                i.getStatus().name(),
                i.getGoogleEventId() != null,
                i.getParticipants().stream()
                        .map(p -> new InterviewResponse.Participant(p.getKind().name(), p.getManagerId(), p.getEmployeeId(),
                                p.getName(), p.getEmail()))
                        .toList(),
                i.getCreatedAt()
        );
    }
}
