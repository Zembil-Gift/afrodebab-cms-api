package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findAllByJobId(Long jobId);
    List<JobApplication> findAllByJobIdAndIdIn(Long jobId, Collection<Long> ids);
    List<JobApplication> findAllByJobIdAndStatusIn(Long jobId, Collection<JobApplication.ApplicationStatus> statuses);
    List<JobApplication> findAllByJobIdAndIdNotInAndStatusIn(Long jobId,
                                                             Collection<Long> ids,
                                                             Collection<JobApplication.ApplicationStatus> statuses);
    Optional<JobApplication> findByIdAndJobId(Long id, Long jobId);
    boolean existsByJobIdAndStatus(Long jobId, JobApplication.ApplicationStatus status);
    boolean existsByJobIdAndEmailIgnoreCase(Long jobId, String email);
    boolean existsByJobIdAndPhoneNumber(Long jobId, String phoneNumber);

    @Query("""
        SELECT a FROM JobApplication a
        WHERE a.aiOverviewStatus = 'PENDING'
        AND a.resumeUrl IS NOT NULL AND a.resumeUrl <> ''
        AND a.aiOverviewAttemptCount < :maxAttempts
        ORDER BY a.createdAt ASC LIMIT 1
    """)
    Optional<JobApplication> findFirstPendingAiOverview(@Param("maxAttempts") int maxAttempts);
}
