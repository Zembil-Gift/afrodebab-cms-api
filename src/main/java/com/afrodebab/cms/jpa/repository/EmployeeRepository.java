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

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    List<Employee> findAllByGithubUsernameIgnoreCase(String githubUsername);

    List<Employee> findAllByActiveTrueAndSalaryEffectiveDateIsNotNullAndSalaryAmountMinorIsNotNull();
    List<Employee> findAllByActiveTrueOrderByNameAsc();
    List<Employee> findAllByActiveTrueAndIdNotOrderByNameAsc(Long id);

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

    @Query(
            value = """
                    select *
                    from employees e
                    where (:department is null or lower(cast(e.department as text)) = lower(cast(:department as text)))
                      and (:role is null or lower(cast(e.role as text)) = lower(cast(:role as text)))
                    /*#pageable*/
                    """,
            countQuery = """
                    select count(*)
                    from employees e
                    where (:department is null or lower(cast(e.department as text)) = lower(cast(:department as text)))
                      and (:role is null or lower(cast(e.role as text)) = lower(cast(:role as text)))
                    """,
            nativeQuery = true
    )
    Page<Employee> findAllByDepartmentAndRole(
            @Param("department") String department,
            @Param("role") String role,
            Pageable pageable
    );

    @Query(
            value = """
                    select *
                    from employees e
                    where e.github_username is not null
                      and btrim(e.github_username) <> ''
                    /*#pageable*/
                    """,
            countQuery = """
                    select count(*)
                    from employees e
                    where e.github_username is not null
                      and btrim(e.github_username) <> ''
                    """,
            nativeQuery = true
    )
    Page<Employee> findAllWithGithubUsername(Pageable pageable);

    @Query(
            value = """
                    select *
                    from employees e
                    where e.trello_username is not null
                      and btrim(e.trello_username) <> ''
                    /*#pageable*/
                    """,
            countQuery = """
                    select count(*)
                    from employees e
                    where e.trello_username is not null
                      and btrim(e.trello_username) <> ''
                    """,
            nativeQuery = true
    )
    Page<Employee> findAllWithTrelloUsername(Pageable pageable);

    @Query(
            value = """
                    select *
                    from employees e
                    where e.telegram_username is not null
                      and btrim(e.telegram_username) <> ''
                    /*#pageable*/
                    """,
            countQuery = """
                    select count(*)
                    from employees e
                    where e.telegram_username is not null
                      and btrim(e.telegram_username) <> ''
                    """,
            nativeQuery = true
    )
    Page<Employee> findAllWithTelegramUsername(Pageable pageable);

    @Query(
            value = """
                    select *
                    from employees e
                    where (e.github_username is not null and btrim(e.github_username) <> '')
                       or (e.trello_username is not null and btrim(e.trello_username) <> '')
                       or (e.telegram_username is not null and btrim(e.telegram_username) <> '')
                    /*#pageable*/
                    """,
            countQuery = """
                    select count(*)
                    from employees e
                    where (e.github_username is not null and btrim(e.github_username) <> '')
                       or (e.trello_username is not null and btrim(e.trello_username) <> '')
                       or (e.telegram_username is not null and btrim(e.telegram_username) <> '')
                    """,
            nativeQuery = true
    )
    Page<Employee> findAllWithConnectedAccounts(Pageable pageable);
}
