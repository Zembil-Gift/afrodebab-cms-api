package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.AdminPeerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminPeerReviewRepository extends JpaRepository<AdminPeerReview, Long> {
    Optional<AdminPeerReview> findByRevieweeIdAndPeriodId(Long revieweeId, Long periodId);
    List<AdminPeerReview> findAllByPeriodId(Long periodId);
}
