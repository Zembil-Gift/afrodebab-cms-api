package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    List<Employee> findAllByGithubUsernameIgnoreCase(String githubUsername);

    List<Employee> findAllByActiveTrueAndSalaryEffectiveDateIsNotNullAndSalaryAmountMinorIsNotNull();
    List<Employee> findAllByActiveTrueOrderByNameAsc();
    List<Employee> findAllByActiveTrueAndIdNotOrderByNameAsc(Long id);
    long countBySubOrganizationId(Long subOrganizationId);

    @Query("SELECT e.id FROM Employee e WHERE e.subOrganization.id = :subOrganizationId")
    Set<Long> findIdsBySubOrganizationId(@Param("subOrganizationId") Long subOrganizationId);
    Page<Employee> findAllBySubOrganizationId(Long subOrganizationId, Pageable pageable);
    List<Employee> findAllByActiveTrueAndSubOrganizationIdOrderByNameAsc(Long subOrganizationId);
    List<Employee> findAllByActiveTrueAndSubOrganizationIdAndIdNotOrderByNameAsc(Long subOrganizationId, Long id);

    @Query("""
            SELECT e FROM Employee e
            WHERE e.active = true
              AND e.id != :reviewerId
              AND e.id NOT IN (
                  SELECT pr.reviewee.id FROM PeerReview pr
                  WHERE pr.reviewer.id = :reviewerId
                    AND pr.periodStart = :periodStart
                    AND pr.periodEnd = :periodEnd
                    AND pr.rating IS NOT NULL
              )
            ORDER BY e.name ASC
            """)
    List<Employee> findUnreviewedEmployees(
            @Param("reviewerId") Long reviewerId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    // JPQL (not native) so the @TenantId filter applies. department/role must be lower-cased by
    // the caller: lower(:param) on a null binds as bytea in Postgres.
    @Query("""
            SELECT e FROM Employee e
            WHERE (:department IS NULL OR lower(e.department) = :department)
              AND (:role IS NULL OR lower(e.role) = :role)
              AND (:subOrganizationId IS NULL OR e.subOrganization.id = :subOrganizationId)
            """)
    Page<Employee> findAllByDepartmentAndRole(
            @Param("department") String department,
            @Param("role") String role,
            @Param("subOrganizationId") Long subOrganizationId,
            Pageable pageable
    );

    // JPQL (not native) so the @TenantId filter applies; native SQL returned other orgs' employees.
    @Query("SELECT e FROM Employee e WHERE (e.githubUsername IS NOT NULL AND trim(e.githubUsername) <> '')")
    Page<Employee> findAllWithGithubUsername(Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE (e.trelloUsername IS NOT NULL AND trim(e.trelloUsername) <> '')")
    Page<Employee> findAllWithTrelloUsername(Pageable pageable);

    @Query("""
            SELECT e FROM Employee e
            WHERE (e.telegramUsername IS NOT NULL AND trim(e.telegramUsername) <> '')
              AND (:subOrganizationId IS NULL OR e.subOrganization.id = :subOrganizationId)
            """)
    Page<Employee> findAllWithTelegramUsername(@Param("subOrganizationId") Long subOrganizationId, Pageable pageable);

    @Query("""
            SELECT e FROM Employee e
            WHERE ((e.githubUsername IS NOT NULL AND trim(e.githubUsername) <> '')
               OR (e.trelloUsername IS NOT NULL AND trim(e.trelloUsername) <> '')
               OR (e.telegramUsername IS NOT NULL AND trim(e.telegramUsername) <> ''))
              AND (:subOrganizationId IS NULL OR e.subOrganization.id = :subOrganizationId)
            """)
    Page<Employee> findAllWithConnectedAccounts(@Param("subOrganizationId") Long subOrganizationId, Pageable pageable);
}
