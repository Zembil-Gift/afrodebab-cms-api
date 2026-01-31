package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.ApplyRequest;
import com.afrodebab.cms.dto.ApplyResponse;
import com.afrodebab.cms.dto.JobApplicationAdminResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repo;
    private final JobService jobService;

    public JobApplicationService(JobApplicationRepository repo, JobService jobService) {
        this.repo = repo;
        this.jobService = jobService;
    }

    @Transactional
    public ApplyResponse apply(Long jobId, ApplyRequest req) {
        Job job = jobService.getEntityOrThrow(jobId);

        if (job.getStatus() != Job.Status.OPEN) {
            throw new BadRequestException("Job is not open for applications");
        }

        JobApplication app = new JobApplication();
        app.setJob(job);
        app.setFullName(req.fullName());
        app.setEmail(req.email());
        app.setPhoneNumber(req.phoneNumber());
        app.setGithubUrl(req.githubUrl());

        repo.save(app);
        return new ApplyResponse(app.getId(), "Application submitted");
    }

    public List<JobApplicationAdminResponse> listAll() {
        return repo.findAll().stream().map(this::toAdmin).toList();
    }

    public List<JobApplicationAdminResponse> listByJobId(Long jobId) {
        return repo.findAllByJobId(jobId).stream().map(this::toAdmin).toList();
    }

    private JobApplicationAdminResponse toAdmin(JobApplication a) {
        return new JobApplicationAdminResponse(
                a.getId(),
                a.getJob().getId(),
                a.getFullName(),
                a.getEmail(),
                a.getPhoneNumber(),
                a.getGithubUrl(),
                a.getCreatedAt()
        );
    }
}

