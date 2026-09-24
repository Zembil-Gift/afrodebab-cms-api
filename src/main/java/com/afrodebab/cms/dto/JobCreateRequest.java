package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplicationField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record JobCreateRequest(
        @NotBlank(message="title is required") String title,
        String slug,
        String department,
        @NotNull(message="employmentType is required") Job.EmploymentType employmentType,
        String location,
        @NotBlank(message="description is required") String description,
        Job.Status status,
        @Size(max = 40, message = "experienceLevel is too long") String experienceLevel,
        @Size(max = 120, message = "salaryRange is too long") String salaryRange,
        LocalDate applicationDeadline,
        List<JobApplicationField> applicationFields
) {}
