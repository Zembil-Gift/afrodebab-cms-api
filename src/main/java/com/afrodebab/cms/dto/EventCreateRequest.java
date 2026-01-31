package com.afrodebab.cms.dto;


import com.afrodebab.cms.jpa.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record EventCreateRequest(
        @NotBlank(message="title is required") String title,
        String slug,
        @NotBlank(message="description is required") String description,
        @NotNull(message="eventType is required") Event.EventType eventType,
        String location,
        @NotNull(message="startDate is required") Instant startDate,
        Instant endDate,
        String registrationUrl,
        Event.Status status
) {}

