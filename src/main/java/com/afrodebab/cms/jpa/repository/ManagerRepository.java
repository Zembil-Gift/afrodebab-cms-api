package com.afrodebab.cms.jpa.repository;


import com.afrodebab.cms.jpa.entity.Manager;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByEmailIgnoreCase(String email);
    List<Manager> findAllByActiveTrue();
    List<Manager> findAllByActiveTrueAndTrelloTokenIsNotNull();
    List<Manager> findAllByActiveTrueAndGithubTokenIsNotNull();
    List<Manager> findAllByRole(Manager.ManagerRole role);
    long countBySubOrganizationIdAndRole(Long subOrganizationId, Manager.ManagerRole role);
    Optional<Manager> findByIdAndRole(Long id, Manager.ManagerRole role);

    // Fetches the sub-org eagerly so callers outside a transaction can read its name.
    @EntityGraph(attributePaths = "subOrganization")
    Optional<Manager> findWithSubOrganizationByEmailIgnoreCase(String email);
}
