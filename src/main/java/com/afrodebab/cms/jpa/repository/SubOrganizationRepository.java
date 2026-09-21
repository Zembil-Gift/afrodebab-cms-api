package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.SubOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubOrganizationRepository extends JpaRepository<SubOrganization, Long> {
    List<SubOrganization> findAllByOrderByCreatedAtAsc();
    Optional<SubOrganization> findBySlugIgnoreCase(String slug);
    Optional<SubOrganization> findByNameIgnoreCase(String name);
    Optional<SubOrganization> findByIsDefaultTrue();
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsByNameIgnoreCase(String name);
}
