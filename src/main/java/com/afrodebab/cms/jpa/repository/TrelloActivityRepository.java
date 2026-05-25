package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.TrelloActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrelloActivityRepository extends JpaRepository<TrelloActivity, Long> {
    Page<TrelloActivity> findAllByOrderByActivityTimestampDesc(Pageable pageable);
    Page<TrelloActivity> findAllByEmployeeIdOrderByActivityTimestampDesc(Long employeeId, Pageable pageable);
    List<TrelloActivity> findAllByEmployeeId(Long employeeId);
    Optional<TrelloActivity> findByActivityId(String activityId);
    boolean existsByActivityId(String activityId);
}
