package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.JobCreateRequest;
import com.afrodebab.cms.dto.JobResponse;
import com.afrodebab.cms.dto.JobUpdateRequest;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.repository.JobRepository;
import com.afrodebab.cms.util.SlugUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Transactional
public class JobService {
    // ponytail: deadlines are judged in Ethiopian time; use the org's zone if orgs span time zones.
    private static final ZoneId DEADLINE_ZONE = ZoneId.of("Africa/Addis_Ababa");

    private final JobRepository repo;
    private final JobApplicationFormService formService;

    public JobService(JobRepository repo, JobApplicationFormService formService) {
        this.repo = repo;
        this.formService = formService;
    }

    // public: OPEN jobs still within their application deadline
    @Transactional(readOnly = true)
    public Page<JobResponse> listOpen(Pageable pageable) {
        return repo.findAcceptingApplications(LocalDate.now(DEADLINE_ZONE), pageable).map(this::toResponse);
    }

    // manager: list ALL of the current tenant's jobs (any status, incl. DRAFT).
    // Tenant-scoped automatically by Hibernate's @TenantId filter.
    @Transactional(readOnly = true)
    public Page<JobResponse> listAll(Pageable pageable) {
        return repo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse getOne(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public JobResponse getBySlugPublic(String slug) {
        Job j = repo.findBySlug(slug).orElseThrow(() -> new NotFoundException("Job not found"));
        // Optional: hide DRAFT from public
        if (j.getStatus() == Job.Status.DRAFT) throw new NotFoundException("Job not found");
        return toResponse(j);
    }

    // admin
    public JobResponse create(JobCreateRequest req) {
        Job j = new Job();
        j.setTitle(req.title());
        j.setDepartment(req.department());
        j.setEmploymentType(req.employmentType());
        j.setLocation(req.location());
        j.setDescription(req.description());
        j.setStatus(req.status() == null ? Job.Status.DRAFT : req.status());
        j.setExperienceLevel(trimToNull(req.experienceLevel()));
        j.setSalaryRange(trimToNull(req.salaryRange()));
        j.setApplicationDeadline(req.applicationDeadline());
        j.setApplicationFields(formService.normalizeFields(req.applicationFields()));
        String baseSlug = (req.slug() != null && !req.slug().isBlank())
                ? SlugUtil.toSlug(req.slug())
                : SlugUtil.toSlug(req.title());
        j.setSlug(uniqueSlug(baseSlug));

        repo.save(j);
        return toResponse(j);
    }

    public JobResponse update(Long id, JobUpdateRequest req) {
        Job j = repo.findById(id).orElseThrow(() -> new NotFoundException("Job not found"));

        if (req.title() != null) j.setTitle(req.title());
        if (req.department() != null) j.setDepartment(req.department());
        if (req.employmentType() != null) j.setEmploymentType(req.employmentType());
        if (req.location() != null) j.setLocation(req.location());
        if (req.description() != null) j.setDescription(req.description());
        if (req.status() != null) j.setStatus(req.status());
        j.setExperienceLevel(trimToNull(req.experienceLevel()));
        j.setSalaryRange(trimToNull(req.salaryRange()));
        j.setApplicationDeadline(req.applicationDeadline());
        if (req.applicationFields() != null) j.setApplicationFields(formService.normalizeFields(req.applicationFields()));

        // Re-slug only on an actual change: uniqueSlug would treat the job's own slug as taken.
        if (req.slug() != null && !req.slug().isBlank() && !SlugUtil.toSlug(req.slug()).equals(j.getSlug())) {
            j.setSlug(uniqueSlug(SlugUtil.toSlug(req.slug())));
        }

        repo.save(j);
        return toResponse(j);
    }

    /** OPEN and not past its deadline (the deadline day itself still counts). */
    public boolean isAcceptingApplications(Job j) {
        return j.getStatus() == Job.Status.OPEN
                && (j.getApplicationDeadline() == null || !LocalDate.now(DEADLINE_ZONE).isAfter(j.getApplicationDeadline()));
    }

    public Job getEntityOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Job not found"));
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int i = 2;
        while (repo.existsBySlug(slug)) slug = base + "-" + (i++);
        return slug;
    }

    private JobResponse toResponse(Job j) {
        return new JobResponse(
                j.getId(), j.getTitle(), j.getSlug(), j.getDepartment(),
                j.getEmploymentType(), j.getLocation(), j.getDescription(), j.getStatus(),
                j.getCreatedAt(), j.getExperienceLevel(), j.getSalaryRange(), j.getApplicationDeadline(),
                isAcceptingApplications(j), j.getApplicationFields()
        );
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

