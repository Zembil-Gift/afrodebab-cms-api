package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.InterviewFeedback;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, Long> {

    Optional<InterviewFeedback> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InterviewFeedback> findForUpdateByTokenHash(String tokenHash);

    List<InterviewFeedback> findAllByInterviewIdOrderByIdAsc(Long interviewId);

    // Native so the anonymous link can be resolved to its org before any tenant is in scope.
    @Query(value = "SELECT organization_id FROM interview_feedback WHERE token_hash = :tokenHash", nativeQuery = true)
    Optional<Long> findOrganizationIdByTokenHash(String tokenHash);
}
