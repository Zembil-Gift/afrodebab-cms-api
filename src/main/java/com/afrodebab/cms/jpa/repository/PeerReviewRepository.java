package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {
    Optional<PeerReview> findByReviewerIdAndRevieweeIdAndPrincipleIdAndPeriodStartAndPeriodEnd(
            Long reviewerId,
            Long revieweeId,
            Long principleId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<PeerReview> findAllByRevieweeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            Long revieweeId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<PeerReview> findAllByPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<PeerReview> findAllByReviewerId(Long reviewerId);
}
