package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Event;

import java.time.Instant;

public record EventUpdateRequest(
        String title,
        String slug,
        String description,
        Event.EventType eventType,
        String location,
        Instant startDate,
        Instant endDate,
        String registrationUrl,
        Event.Status status
) {}
