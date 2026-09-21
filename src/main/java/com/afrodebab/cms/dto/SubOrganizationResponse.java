package com.afrodebab.cms.dto;

import java.time.Instant;
import java.time.LocalTime;

public record SubOrganizationResponse(
        Long id,
        String name,
        String slug,
        boolean isDefault,
        Double latitude,
        Double longitude,
        Integer geoRadiusM,
        String addressLabel,
        LocalTime entryTime,
        LocalTime exitTime,
        LocalTime lunchStartTime,
        LocalTime lunchEndTime,
        Integer graceMinutes,
        Integer maxLunchBreakMinutes,
        long employeeCount,
        long viceManagerCount,
        Instant createdAt,
        Instant updatedAt
) {}
