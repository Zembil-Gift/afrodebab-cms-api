package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Job;

public record JobUpdateRequest(
        String title,
        String slug,
        String department,
        Job.EmploymentType employmentType,
        String location,
        String description,
        Job.Status status
) {}

