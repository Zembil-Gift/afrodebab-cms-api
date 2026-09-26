package com.afrodebab.cms.dto;

/** The logged-in manager's identity and their organization (name + URL slug). */
public record ManagerMeResponse(String email, Long orgId, String orgName, String orgSlug) {}
