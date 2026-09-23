package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    @Query("SELECT r FROM PeerReview r WHERE r.createdAt >= :from AND r.createdAt < :to ORDER BY r.createdAt DESC")
    List<PeerReview> findSubmitted(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT r FROM PeerReview r WHERE r.reviewee.id = :revieweeId AND r.createdAt >= :from AND r.createdAt < :to ORDER BY r.createdAt DESC")
    List<PeerReview> findSubmittedForReviewee(@Param("revieweeId") Long revieweeId, @Param("from") Instant from, @Param("to") Instant to);

    /**
     * Reviews submitted between two dates (inclusive, UTC). Reports use this instead of exact
     * review-period dates so reviews from a custom period still count towards the month they were given.
     */
    default List<PeerReview> findSubmittedBetween(Long revieweeId, LocalDate start, LocalDate end) {
        Instant from = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return revieweeId == null ? findSubmitted(from, to) : findSubmittedForReviewee(revieweeId, from, to);
    }

    // Native so it bypasses @TenantId filtering: principles are shared, so a rating from any
    // organization counts when the platform admin deletes one.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM peer_reviews WHERE principle_id = :principleId)",
            nativeQuery = true)
    boolean existsByPrincipleIdInAnyOrganization(@Param("principleId") Long principleId);

    List<PeerReview> findAllByRevieweeId(Long revieweeId);
}
