package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCase(String slug);
}
