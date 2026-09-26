package com.afrodebab.cms.controller;

import com.afrodebab.cms.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** iCalendar endpoints; ResponseEntity is only used to set the text/calendar content type. */
@Tag(name = "Public - event")
@RestController
@RequestMapping("/public/{orgSlug}")
public class EventCalendarPublicController {
    private static final MediaType TEXT_CALENDAR = MediaType.parseMediaType("text/calendar; charset=utf-8");

    private final EventService service;

    public EventCalendarPublicController(EventService service) {
        this.service = service;
    }

    @GetMapping("/events.ics")
    public ResponseEntity<String> feed() {
        return ResponseEntity.ok().contentType(TEXT_CALENDAR).body(service.calendarFeed());
    }

    @GetMapping("/events/{slug}/calendar.ics")
    public ResponseEntity<String> event(@PathVariable String slug) {
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + ".ics\"")
                .body(service.calendarFor(slug));
    }
}
