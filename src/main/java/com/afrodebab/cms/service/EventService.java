package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.EventCreateRequest;
import com.afrodebab.cms.dto.EventResponse;
import com.afrodebab.cms.dto.EventUpdateRequest;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Event;
import com.afrodebab.cms.jpa.repository.EventRepository;
import com.afrodebab.cms.util.SlugUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository repo;

    public EventService(EventRepository repo) { this.repo = repo; }

    // public
    @Transactional(readOnly = true)
    public Page<EventResponse> listPublished(Pageable pageable) {
        return repo.findAllByStatus(Event.Status.PUBLISHED, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getPublishedBySlug(String slug) {
        Event e = repo.findBySlugAndStatus(slug, Event.Status.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return toResponse(e);
    }

    // admin
    public EventResponse create(EventCreateRequest req) {
        Event e = new Event();
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setEventType(req.eventType());
        e.setLocation(req.location());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        e.setRegistrationUrl(req.registrationUrl());
        e.setStatus(req.status() == null ? Event.Status.DRAFT : req.status());

        String baseSlug = (req.slug() != null && !req.slug().isBlank())
                ? SlugUtil.toSlug(req.slug())
                : SlugUtil.toSlug(req.title());
        e.setSlug(uniqueSlug(baseSlug));

        repo.save(e);
        return toResponse(e);
    }

    public EventResponse update(Long id, EventUpdateRequest req) {
        Event e = repo.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));

        if (req.title() != null) e.setTitle(req.title());
        if (req.description() != null) e.setDescription(req.description());
        if (req.eventType() != null) e.setEventType(req.eventType());
        if (req.location() != null) e.setLocation(req.location());
        if (req.startDate() != null) e.setStartDate(req.startDate());
        if (req.endDate() != null) e.setEndDate(req.endDate());
        if (req.registrationUrl() != null) e.setRegistrationUrl(req.registrationUrl());
        if (req.status() != null) e.setStatus(req.status());

        if (req.slug() != null) e.setSlug(uniqueSlug(SlugUtil.toSlug(req.slug())));

        repo.save(e);
        return toResponse(e);
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int i = 2;
        while (repo.existsBySlug(slug)) slug = base + "-" + (i++);
        return slug;
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(), e.getTitle(), e.getSlug(), e.getDescription(),
                e.getEventType(), e.getLocation(), e.getStartDate(), e.getEndDate(),
                e.getRegistrationUrl(), e.getStatus()
        );
    }
}

