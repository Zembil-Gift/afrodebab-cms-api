package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.AiOverviewResponse;
import com.afrodebab.cms.dto.ApplyRequest;
import com.afrodebab.cms.dto.ApplyResponse;
import com.afrodebab.cms.dto.HireCandidateRequest;
import com.afrodebab.cms.dto.JobApplicationAdminResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.JobApplicationRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobApplicationService {

    // Ethio telecom (9…) and Safaricom (7…) mobiles, typed as 09…, 9…, 2519… or +2519…
    private static final Pattern ET_PHONE = Pattern.compile("^(?:\\+?251|0)?([79]\\d{8})$");

    private final JobApplicationRepository repo;
    private final JobService jobService;
    private final CloudflareR2Service cloudflareR2Service;
    private final EmployeeService employeeService;
    private final EmailNotificationService emailNotificationService;
    private final AiOverviewService aiOverviewService;
    private final ManagerRepository managerRepo;
    private final JobApplicationFormService formService;

    public JobApplicationService(JobApplicationRepository repo,
                                 JobService jobService,
                                 CloudflareR2Service cloudflareR2Service,
                                 EmployeeService employeeService,
                                 EmailNotificationService emailNotificationService,
                                 AiOverviewService aiOverviewService,
                                 ManagerRepository managerRepo,
                                 JobApplicationFormService formService) {
        this.repo = repo;
        this.jobService = jobService;
        this.cloudflareR2Service = cloudflareR2Service;
        this.employeeService = employeeService;
        this.emailNotificationService = emailNotificationService;
        this.aiOverviewService = aiOverviewService;
        this.managerRepo = managerRepo;
        this.formService = formService;
    }

    @Transactional
    public ApplyResponse apply(Long jobId, ApplyRequest req) {
        Job job = getJobAcceptingApplicationsOrThrow(jobId);
        // JSON has no files, so a form with required file fields must use the multipart endpoint.
        var pending = formService.validateAnswers(job, Map.of(), Map.of());
        JobApplication app = createApplication(job, req);
        app.setAnswers(formService.store(app.getId(), pending));
        repo.save(app);
        return new ApplyResponse(app.getId(), "Application submitted");
    }

    @Transactional
    public ApplyResponse applyWithResume(Long jobId, ApplyRequest req, MultipartFile resumeFile,
                                         Map<String, String[]> params, Map<String, MultipartFile> files) {
        Job job = getJobAcceptingApplicationsOrThrow(jobId);
        // Validate everything before saving or uploading anything.
        formService.checkResume(resumeFile);
        var pending = formService.validateAnswers(job, params, files);

        JobApplication app = createApplication(job, req);
        app.setResumeUrl(cloudflareR2Service.uploadJobApplicationResume(app.getId(), resumeFile));
        app.setAnswers(formService.store(app.getId(), pending));
        repo.save(app);
        return new ApplyResponse(app.getId(), "Application submitted");
    }

    private Job getJobAcceptingApplicationsOrThrow(Long jobId) {
        Job job = jobService.getEntityOrThrow(jobId);
        if (!jobService.isAcceptingApplications(job)) {
            throw new BadRequestException("This job is no longer accepting applications");
        }
        return job;
    }

    @Transactional
    public JobApplicationAdminResponse setUnderReview(Long applicationId) {
        JobApplication app = repo.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Job application not found"));
        if (app.getStatus() != JobApplication.ApplicationStatus.APPLIED) {
            throw new BadRequestException("Only APPLIED applications can be moved to UNDER_REVIEW");
        }
        app.setStatus(JobApplication.ApplicationStatus.UNDER_REVIEW);
        repo.save(app);
        return toAdmin(app);
    }

    @Transactional
    public List<JobApplicationAdminResponse> selectInterviewCandidates(Long jobId, List<Long> applicationIds) {
        jobService.getEntityOrThrow(jobId);

        if (applicationIds == null || applicationIds.isEmpty()) {
            throw new BadRequestException("applicationIds is required");
        }

        Set<Long> uniqueIds = new HashSet<>(applicationIds);
        List<JobApplication> selected = repo.findAllByJobIdAndIdIn(jobId, uniqueIds);
        if (selected.size() != uniqueIds.size()) {
            throw new NotFoundException("One or more selected applications were not found for this job");
        }

        for (JobApplication app : selected) {
            if (app.getStatus() != JobApplication.ApplicationStatus.APPLIED
                    && app.getStatus() != JobApplication.ApplicationStatus.UNDER_REVIEW) {
                throw new BadRequestException("Selected applications must be APPLIED or UNDER_REVIEW");
            }
        }

        for (JobApplication app : selected) {
            app.setStatus(JobApplication.ApplicationStatus.SELECTED_FOR_INTERVIEW);
        }

        repo.saveAll(selected);
        return repo.findAllByJobId(jobId).stream().map(this::toAdmin).toList();
    }

    @Transactional
    public JobApplicationAdminResponse hireCandidate(Long jobId, HireCandidateRequest req) {
        jobService.getEntityOrThrow(jobId);

        JobApplication selectedCandidate = repo.findByIdAndJobId(req.applicationId(), jobId)
                .orElseThrow(() -> new NotFoundException("Job application not found"));
        if (selectedCandidate.getStatus() == JobApplication.ApplicationStatus.HIRED) {
            throw new BadRequestException("Candidate is already hired");
        }

        // Jobs are org-wide; the manager picks the sub-organization at hire time (null = the main one).
        Long targetSubOrgId = req.subOrganizationId();

        Employee employee = employeeService.createEntityFromHiredApplication(
                selectedCandidate.getFullName(),
                selectedCandidate.getEmail(),
                req.phone(),
                req.position(),
                targetSubOrgId,
                req.salaryDate(),
                req.salaryAmountMinor()
        );

        selectedCandidate.setStatus(JobApplication.ApplicationStatus.HIRED);
        selectedCandidate.setHiredEmployee(employee);
        repo.save(selectedCandidate);

        return toAdmin(selectedCandidate);
    }

    @Transactional
    public List<JobApplicationAdminResponse> sendRejectionLetters(Long jobId) {
        Job job = jobService.getEntityOrThrow(jobId);
        if (!repo.existsByJobIdAndStatus(jobId, JobApplication.ApplicationStatus.HIRED)) {
            throw new BadRequestException("At least one candidate must be hired before sending rejections");
        }

        List<JobApplication> applications = repo.findAllByJobId(jobId);
        List<JobApplication> rejected = new ArrayList<>();
        String jobTitle = job.getTitle();

        for (JobApplication app : applications) {
            if (app.getStatus() == JobApplication.ApplicationStatus.HIRED
                    || app.getStatus() == JobApplication.ApplicationStatus.REJECTED) {
                continue;
            }
            app.setStatus(JobApplication.ApplicationStatus.REJECTED);
            app.setHiredEmployee(null);
            emailNotificationService.queueHiringRejectedPostInterviewEmail(
                    app.getEmail(),
                    app.getFullName(),
                    jobTitle
            );
            rejected.add(app);
        }

        repo.saveAll(rejected);
        return applications.stream().map(this::toAdmin).toList();
    }

    @Transactional(readOnly = true)
    public List<JobApplicationAdminResponse> listAll() {
        return repo.findAll().stream().map(this::toAdmin).toList();
    }

    @Transactional(readOnly = true)
    public List<JobApplicationAdminResponse> listByJobId(Long jobId) {
        return repo.findAllByJobId(jobId).stream().map(this::toAdmin).toList();
    }

    @Transactional(readOnly = true)
    public JobApplication getEntityOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Job application not found"));
    }

    @Transactional(readOnly = true)
    public AiOverviewResponse getAiOverview(Long id) {
        JobApplication app = getEntityOrThrow(id);
        return new AiOverviewResponse(
                app.getId(),
                app.getFullName(),
                app.getJob().getTitle(),
                app.getAiOverviewText(),
                app.getAiOverviewStatus() == null ? "PENDING" : app.getAiOverviewStatus().name(),
                app.getAiOverviewError(),
                app.getAiOverviewAttemptCount(),
                app.getAiOverviewCompletedAt()
        );
    }

    private JobApplication createApplication(Job job, ApplyRequest req) {
        Long jobId = job.getId();
        String normalizedEmail = normalizeEmail(req.email());
        String normalizedPhoneNumber = normalizePhoneNumber(req.phoneNumber());
        if (repo.existsByJobIdAndEmailIgnoreCase(jobId, normalizedEmail)) {
            throw new BadRequestException("You have already applied for this job with this email");
        }
        if (normalizedPhoneNumber != null && repo.existsByJobIdAndPhoneNumber(jobId, normalizedPhoneNumber)) {
            throw new BadRequestException("You have already applied for this job with this phone number");
        }

        JobApplication app = new JobApplication();
        app.setJob(job);
        app.setFullName(req.fullName());
        app.setEmail(normalizedEmail);
        app.setPhoneNumber(normalizedPhoneNumber);
        app.setGithubUrl(req.githubUrl());
        app.setStatus(JobApplication.ApplicationStatus.APPLIED);

        try {
            repo.saveAndFlush(app);
        } catch (DataIntegrityViolationException e) {
            // Unique (job, email) / (job, phone) index: a concurrent double-submit got past the checks above.
            throw new BadRequestException("You have already applied for this job");
        }
        aiOverviewService.queue(app);
        managerRepo.findAllByActiveTrueAndRole(Manager.ManagerRole.MANAGER)
                .forEach(manager -> emailNotificationService.queueManagerNewJobApplicationEmail(
                        manager.getEmail(), manager.getName(), app.getFullName(), app.getEmail(), job.getTitle()));
        return app;
    }

    private JobApplicationAdminResponse toAdmin(JobApplication a) {
        return new JobApplicationAdminResponse(
                a.getId(),
                a.getJob().getId(),
                a.getFullName(),
                a.getEmail(),
                a.getPhoneNumber(),
                a.getGithubUrl(),
                a.getResumeUrl(),
                a.getStatus(),
                a.getHiredEmployee() == null ? null : a.getHiredEmployee().getId(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getAnswers()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Canonical +2519XXXXXXXX / +2517XXXXXXXX so duplicate checks match however it was typed. */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        Matcher m = ET_PHONE.matcher(phoneNumber.replaceAll("[\\s().-]", ""));
        if (!m.matches()) {
            throw new BadRequestException("Enter a valid Ethiopian phone number, e.g. 0911 234 567 or +251 911 234 567");
        }
        return "+251" + m.group(1);
    }
}
