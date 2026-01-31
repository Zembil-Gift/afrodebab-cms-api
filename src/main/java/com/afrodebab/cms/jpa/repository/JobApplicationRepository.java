package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findAllByJobId(Long jobId);
}
