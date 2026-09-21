package com.afrodebab.cms.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record SubOrganizationCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        String name,

        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase alphanumeric with hyphens")
        String slug,

        @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
        Double longitude,

        @Positive(message = "geoRadiusM must be positive")
        Integer geoRadiusM,

        @Size(max = 500, message = "addressLabel must not exceed 500 characters")
        String addressLabel,

        LocalTime entryTime,
        LocalTime exitTime,
        LocalTime lunchStartTime,
        LocalTime lunchEndTime,

        @Positive(message = "graceMinutes must be positive")
        Integer graceMinutes,

        @Positive(message = "maxLunchBreakMinutes must be positive")
        Integer maxLunchBreakMinutes
) {}
