package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.LeadershipPrinciple;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadershipPrincipleRepository extends JpaRepository<LeadershipPrinciple, Long> {
    List<LeadershipPrinciple> findAllByActiveTrueOrderByIdAsc();
    long countByActiveTrue();
}
