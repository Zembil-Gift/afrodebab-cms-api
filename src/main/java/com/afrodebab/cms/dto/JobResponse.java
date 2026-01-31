package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Job;

public record JobResponse(
        Long id,
        String title,
        String slug,
        String department,
        Job.EmploymentType employmentType,
        String location,
        String description,
        Job.Status status
) {}
