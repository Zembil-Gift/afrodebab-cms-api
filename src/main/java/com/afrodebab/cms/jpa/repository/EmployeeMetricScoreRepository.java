package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.EmployeeMetricScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface EmployeeMetricScoreRepository extends JpaRepository<EmployeeMetricScore, Long> {
    Optional<EmployeeMetricScore> findTopByEmployeeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            Long employeeId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
