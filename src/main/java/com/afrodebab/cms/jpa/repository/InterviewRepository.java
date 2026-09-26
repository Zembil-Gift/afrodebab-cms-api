package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findAllByApplicationIdOrderByStartAtDesc(Long applicationId);

    List<Interview> findAllByApplicationJobIdOrderByStartAtAsc(Long jobId);
}
