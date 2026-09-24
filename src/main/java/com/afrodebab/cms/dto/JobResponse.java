package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplicationField;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record JobResponse(
        Long id,
        String title,
        String slug,
        String department,
        Job.EmploymentType employmentType,
        String location,
        String description,
        Job.Status status,
        Instant createdAt,
        String experienceLevel,
        String salaryRange,
        LocalDate applicationDeadline,
        boolean acceptingApplications,
        List<JobApplicationField> applicationFields
) {}
