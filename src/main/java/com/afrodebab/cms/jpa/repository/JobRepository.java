package com.afrodebab.cms.jpa.repository;


import com.afrodebab.cms.jpa.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findBySlug(String slug);
    Page<Job> findAllByStatus(Job.Status status, Pageable pageable);
    boolean existsBySlug(String slug);
}
