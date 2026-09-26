package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.DefaultLeadershipPrinciple;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefaultLeadershipPrincipleRepository extends JpaRepository<DefaultLeadershipPrinciple, Long> {
    List<DefaultLeadershipPrinciple> findAllByOrderByIdAsc();
    List<DefaultLeadershipPrinciple> findAllByActiveTrueOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
}
