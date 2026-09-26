package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.EventCreateRequest;
import com.afrodebab.cms.dto.EventResponse;
import com.afrodebab.cms.dto.EventUpdateRequest;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Event;
import com.afrodebab.cms.jpa.repository.EventRepository;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import com.afrodebab.cms.util.ICalendar;
import com.afrodebab.cms.util.SlugUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class EventService {

    // Past events stay in subscribers' calendars for a while, then drop out of the feed.
    private static final Duration FEED_HISTORY = Duration.ofDays(90);

    private final EventRepository repo;
    private final OrganizationRepository organizationRepo;
    private final String frontendUrl;

    public EventService(EventRepository repo,
                        OrganizationRepository organizationRepo,
                        @Value("${app.frontend-url:}") String frontendUrl) {
        this.repo = repo;
        this.organizationRepo = organizationRepo;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

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

    /** Subscribable iCalendar feed of the org's published events (Google/Outlook/Apple "subscribe by URL"). */
    @Transactional(readOnly = true)
    public String calendarFeed() {
        Organization org = currentOrgOrThrow();
        List<ICalendar.Event> events = repo
                .findAllByStatusAndStartDateAfterOrderByStartDateAsc(Event.Status.PUBLISHED, Instant.now().minus(FEED_HISTORY))
                .stream().map(e -> toCalendarEvent(e, org)).toList();
        return ICalendar.write(org.getName() + " events", ICalendar.Method.PUBLISH, events);
    }

    /** One published event as a downloadable .ics ("Add to calendar"). */
    @Transactional(readOnly = true)
    public String calendarFor(String slug) {
        Organization org = currentOrgOrThrow();
        Event e = repo.findBySlugAndStatus(slug, Event.Status.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return ICalendar.write(null, ICalendar.Method.PUBLISH, List.of(toCalendarEvent(e, org)));
    }

    // manager: list ALL of the current tenant's events (any status). Tenant-scoped by @TenantId.
    @Transactional(readOnly = true)
    public Page<EventResponse> listAllAdmin(Pageable pageable) {
        return repo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getOne(Long id) {
        Event e = repo.findById(id).orElseThrow(() -> new NotFoundException("Event not found"));
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
        e.setCoverImageUrl(req.coverImageUrl());
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
        if (req.coverImageUrl() != null) e.setCoverImageUrl(req.coverImageUrl());
        if (req.status() != null) e.setStatus(req.status());


        // Re-slug only on an actual change: uniqueSlug would treat the event's own slug as taken.
        if (req.slug() != null && !req.slug().isBlank() && !SlugUtil.toSlug(req.slug()).equals(e.getSlug())) {
            e.setSlug(uniqueSlug(SlugUtil.toSlug(req.slug())));
        }

        repo.save(e);
        return toResponse(e);
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int i = 2;
        while (repo.existsBySlug(slug)) slug = base + "-" + (i++);
        return slug;
    }

    private ICalendar.Event toCalendarEvent(Event e, Organization org) {
        String url = frontendUrl.isEmpty() ? e.getRegistrationUrl() : frontendUrl + "/o/" + org.getSlug() + "/events/" + e.getSlug();
        String location = e.getEventType() == Event.EventType.ONLINE && (e.getLocation() == null || e.getLocation().isBlank())
                ? "Online" : e.getLocation();
        return new ICalendar.Event("event-" + e.getId() + "@mahberix", e.getStartDate(), e.getEndDate(),
                e.getTitle(), e.getDescription(), location, url);
    }

    private Organization currentOrgOrThrow() {
        return organizationRepo.findById(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(), e.getTitle(), e.getSlug(), e.getDescription(),
                e.getEventType(), e.getLocation(), e.getStartDate(), e.getCoverImageUrl(), e.getEndDate(),
                e.getRegistrationUrl(), e.getStatus()
        );
    }
}

