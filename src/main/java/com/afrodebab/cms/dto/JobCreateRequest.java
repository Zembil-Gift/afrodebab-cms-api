package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobCreateRequest(
        @NotBlank(message="title is required") String title,
        String slug,
        String department,
        @NotNull(message="employmentType is required") Job.EmploymentType employmentType,
        String location,
        @NotBlank(message="description is required") String description,
        Job.Status status
) {}
