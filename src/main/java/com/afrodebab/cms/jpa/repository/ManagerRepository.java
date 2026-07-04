package com.afrodebab.cms.jpa.repository;


import com.afrodebab.cms.jpa.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByEmailIgnoreCase(String email);
    List<Manager> findAllByActiveTrue();
}
