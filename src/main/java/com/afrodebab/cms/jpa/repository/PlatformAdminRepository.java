package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.PlatformAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {
    Optional<PlatformAdmin> findByEmailIgnoreCase(String email);
    List<PlatformAdmin> findAllByActiveTrue();
}
