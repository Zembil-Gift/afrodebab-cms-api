package com.afrodebab.cms.dto;

public record LeadershipPrincipleResponse(
        Long id,
        String name,
        String description,
        Boolean isActive
) {
}
