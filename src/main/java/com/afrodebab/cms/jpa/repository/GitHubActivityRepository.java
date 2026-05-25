package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.GitHubActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitHubActivityRepository extends JpaRepository<GitHubActivity, Long> {
    Page<GitHubActivity> findAllByOrderByActivityTimestampDesc(Pageable pageable);
    Page<GitHubActivity> findAllByEmployeeIdOrderByActivityTimestampDesc(Long employeeId, Pageable pageable);
    List<GitHubActivity> findAllByEmployeeId(Long employeeId);
    Optional<GitHubActivity> findByActivityId(String activityId);
    boolean existsByActivityId(String activityId);
}
