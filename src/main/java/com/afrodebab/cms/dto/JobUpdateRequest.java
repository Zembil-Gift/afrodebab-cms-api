package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplicationField;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Null leaves a core field unchanged. experienceLevel, salaryRange and applicationDeadline are
 * optional, so they are always replaced (null clears them); applicationFields is replaced when present.
 */
public record JobUpdateRequest(
        String title,
        String slug,
        String department,
        Job.EmploymentType employmentType,
        String location,
        String description,
        Job.Status status,
        @Size(max = 40, message = "experienceLevel is too long") String experienceLevel,
        @Size(max = 120, message = "salaryRange is too long") String salaryRange,
        LocalDate applicationDeadline,
        List<JobApplicationField> applicationFields
) {}
