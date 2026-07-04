package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Platform-admin request to provision a new organization and its first manager. The manager
 * password is generated server-side and emailed to the manager. {@code requestId}, when
 * present, links this to a self-serve {@code signup_requests} row so it is marked APPROVED.
 */
public record OrgCreateRequest(
        @NotBlank(message = "organization name is required") String name,
        @NotBlank(message = "slug is required")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must be lowercase alphanumeric with hyphens")
        String slug,
        @NotBlank(message = "manager name is required") String managerName,
        @Email(message = "manager email must be valid") @NotBlank(message = "manager email is required") String managerEmail,
        Long requestId
) {}
