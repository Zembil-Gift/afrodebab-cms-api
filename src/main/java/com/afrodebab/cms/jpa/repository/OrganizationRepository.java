package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);

    // Targeted updates so the scheduler never overwrites a profile edit saved at the same time.
    @Modifying
    @Transactional
    @Query("update Organization o set o.lastEmailDispatchAt = :at where o.id = :id")
    void markEmailDispatched(@Param("id") Long id, @Param("at") Instant at);

    @Modifying
    @Transactional
    @Query("update Organization o set o.lastPayrollReminderAt = :at where o.id = :id")
    void markPayrollReminded(@Param("id") Long id, @Param("at") Instant at);
}
