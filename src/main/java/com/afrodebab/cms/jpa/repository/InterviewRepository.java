package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Interview;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findAllByApplicationIdOrderByStartAtDesc(Long applicationId);

    List<Interview> findAllByApplicationJobIdOrderByStartAtAsc(Long jobId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Interview> findForUpdateById(Long id);
}
