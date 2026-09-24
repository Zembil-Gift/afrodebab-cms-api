package com.afrodebab.cms.jpa.repository;


import com.afrodebab.cms.jpa.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findBySlug(String slug);

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = com.afrodebab.cms.jpa.entity.Job.Status.OPEN
        AND (j.applicationDeadline IS NULL OR j.applicationDeadline >= :today)
    """)
    Page<Job> findAcceptingApplications(@Param("today") LocalDate today, Pageable pageable);

    boolean existsBySlug(String slug);
}
