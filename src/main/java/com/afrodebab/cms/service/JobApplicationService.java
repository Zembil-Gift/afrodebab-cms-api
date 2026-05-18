package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.ApplyRequest;
import com.afrodebab.cms.dto.ApplyResponse;
import com.afrodebab.cms.dto.HireCandidateRequest;
import com.afrodebab.cms.dto.JobApplicationAdminResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repo;
    private final JobService jobService;
    private final CloudflareR2Service cloudflareR2Service;
    private final EmployeeService employeeService;
    private final EmailNotificationService emailNotificationService;

    public JobApplicationService(JobApplicationRepository repo,
                                 JobService jobService,
                                 CloudflareR2Service cloudflareR2Service,
                                 EmployeeService employeeService,
                                 EmailNotificationService emailNotificationService) {
        this.repo = repo;
        this.jobService = jobService;
        this.cloudflareR2Service = cloudflareR2Service;
        this.employeeService = employeeService;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public ApplyResponse apply(Long jobId, ApplyRequest req) {
        JobApplication app = createApplication(jobId, req);
        return new ApplyResponse(app.getId(), "Application submitted");
    }

    @Transactional
    public ApplyResponse applyWithResume(Long jobId, ApplyRequest req, MultipartFile resumeFile) {
        JobApplication app = createApplication(jobId, req);
        String resumeUrl = cloudflareR2Service.uploadJobApplicationResume(app.getId(), resumeFile);
        app.setResumeUrl(resumeUrl);
        repo.save(app);
        return new ApplyResponse(app.getId(), "Application submitted");
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
        Job job = jobService.getEntityOrThrow(jobId);
        if (job.getStatus() != Job.Status.CLOSED) {
            throw new BadRequestException("Interview selection is allowed only when the job status is CLOSED");
        }

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

        String jobTitle = job.getTitle();
        for (JobApplication app : selected) {
            app.setStatus(JobApplication.ApplicationStatus.SELECTED_FOR_INTERVIEW);
            emailNotificationService.queueHiringSelectedForInterviewEmail(app.getEmail(), app.getFullName(), jobTitle);
        }

        List<JobApplication> rejected = repo.findAllByJobIdAndIdNotInAndStatusIn(
                jobId,
                uniqueIds,
                List.of(JobApplication.ApplicationStatus.APPLIED, JobApplication.ApplicationStatus.UNDER_REVIEW)
        );
        for (JobApplication app : rejected) {
            app.setStatus(JobApplication.ApplicationStatus.REJECTED);
            emailNotificationService.queueHiringRejectedPreInterviewEmail(app.getEmail(), app.getFullName(), jobTitle);
        }

        repo.saveAll(selected);
        repo.saveAll(rejected);
        return repo.findAllByJobId(jobId).stream().map(this::toAdmin).toList();
    }

    @Transactional
    public JobApplicationAdminResponse hireCandidate(Long jobId, HireCandidateRequest req) {
        Job job = jobService.getEntityOrThrow(jobId);
        if (job.getStatus() != Job.Status.CLOSED) {
            throw new BadRequestException("Hiring is allowed only when the job status is CLOSED");
        }

        if (repo.existsByJobIdAndStatus(jobId, JobApplication.ApplicationStatus.HIRED)) {
            throw new BadRequestException("A candidate is already hired for this job");
        }

        JobApplication selectedCandidate = repo.findByIdAndJobId(req.applicationId(), jobId)
                .orElseThrow(() -> new NotFoundException("Job application not found"));

        Employee employee = employeeService.createEntityFromHiredApplication(
                selectedCandidate.getFullName(),
                selectedCandidate.getEmail(),
                req.phone(),
                req.position(),
                req.salaryDate(),
                req.salaryAmountMinor()
        );

        selectedCandidate.setStatus(JobApplication.ApplicationStatus.HIRED);
        selectedCandidate.setHiredEmployee(employee);
        String jobTitle = job.getTitle();
        emailNotificationService.queueHiringHiredEmail(
                selectedCandidate.getEmail(),
                selectedCandidate.getFullName(),
                jobTitle
        );

        List<JobApplication> applications = repo.findAllByJobId(jobId);
        for (JobApplication app : applications) {
            if (app.getId().equals(selectedCandidate.getId())) {
                continue;
            }
            app.setStatus(JobApplication.ApplicationStatus.REJECTED);
            app.setHiredEmployee(null);
        }
        repo.saveAll(applications);

        return toAdmin(selectedCandidate);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationAdminResponse> listAll() {
        return repo.findAll().stream().map(this::toAdmin).toList();
    }

    @Transactional(readOnly = true)
    public List<JobApplicationAdminResponse> listByJobId(Long jobId) {
        return repo.findAllByJobId(jobId).stream().map(this::toAdmin).toList();
    }

    private JobApplication createApplication(Long jobId, ApplyRequest req) {
        Job job = jobService.getEntityOrThrow(jobId);

        if (job.getStatus() != Job.Status.OPEN) {
            throw new BadRequestException("Job is not open for applications");
        }

        String normalizedEmail = normalizeEmail(req.email());
        String normalizedPhoneNumber = normalizePhoneNumber(req.phoneNumber());
        if (repo.existsByJobIdAndEmailIgnoreCase(jobId, normalizedEmail)) {
            throw new BadRequestException("Email already used for this job");
        }
        if (normalizedPhoneNumber != null && repo.existsByJobIdAndPhoneNumber(jobId, normalizedPhoneNumber)) {
            throw new BadRequestException("Phone number already used for this job");
        }

        JobApplication app = new JobApplication();
        app.setJob(job);
        app.setFullName(req.fullName());
        app.setEmail(normalizedEmail);
        app.setPhoneNumber(normalizedPhoneNumber);
        app.setGithubUrl(req.githubUrl());
        app.setStatus(JobApplication.ApplicationStatus.APPLIED);

        repo.save(app);
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
                a.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String normalized = phoneNumber.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
