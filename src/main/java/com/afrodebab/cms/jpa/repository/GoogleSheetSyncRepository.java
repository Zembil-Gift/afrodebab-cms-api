package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.GoogleSheetSync;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleSheetSyncRepository extends JpaRepository<GoogleSheetSync, Long> {
    Optional<GoogleSheetSync> findByManagerId(Long managerId);
}
