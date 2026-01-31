package com.afrodebab.cms.jpa.repository;


import com.afrodebab.cms.jpa.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findBySlug(String slug);
    Optional<Event> findBySlugAndStatus(String slug, Event.Status status);
    Page<Event> findAllByStatus(Event.Status status, Pageable pageable);
    boolean existsBySlug(String slug);
}

