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

@Service
public class JobService {
    private final JobRepository repo;

    public JobService(JobRepository repo) { this.repo = repo; }

    // public: list OPEN jobs (simple + safe)
    @Transactional(readOnly = true)
    public Page<JobResponse> listOpen(Pageable pageable) {
        return repo.findAllByStatus(Job.Status.OPEN, pageable).map(this::toResponse);
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
        if (req.slug() != null) j.setSlug(uniqueSlug(SlugUtil.toSlug(req.slug())));

        repo.save(j);
        return toResponse(j);
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
                j.getEmploymentType(), j.getLocation(), j.getDescription(), j.getStatus()
        );
    }
}

