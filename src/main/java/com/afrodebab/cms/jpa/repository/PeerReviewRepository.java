package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Native so it bypasses @TenantId filtering: principles are shared, so a rating from any
    // organization counts when the platform admin deletes one.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM peer_reviews WHERE principle_id = :principleId)",
            nativeQuery = true)
    boolean existsByPrincipleIdInAnyOrganization(@Param("principleId") Long principleId);

    List<PeerReview> findAllByRevieweeId(Long revieweeId);
}
