package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Broadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    Page<Broadcast> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Broadcast> findAllBySenderManagerIdOrderByCreatedAtDesc(Long senderManagerId, Pageable pageable);
}
