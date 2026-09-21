package com.afrodebab.cms.dto;

public record ViceManagerMeResponse(
        String email,
        String name,
        Long orgId,
        String orgName,
        String orgSlug,
        Long subOrganizationId,
        String subOrganizationName
) {}
