package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.PeerReviewPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeerReviewPeriodRepository extends JpaRepository<PeerReviewPeriod, Long> {
    Optional<PeerReviewPeriod> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);
    Optional<PeerReviewPeriod> findByNameIgnoreCase(String name);
    List<PeerReviewPeriod> findAllByOrderByPeriodStartDescPeriodEndDesc();
}
