package com.afrodebab.cms.dto;

import java.util.List;

/** Current manager's GitHub connection status + the orgs they selected to track. */
/**
 * {@code subOrganizationLocked} is true for vice managers, whose orgs always credit their own
 * branch (subOrganizationId/Name); managers pick branches per org instead (both null).
 */
public record GitHubConnectionResponse(boolean connected,
                                       String account,
                                       List<GitHubOrgDto> selectedOrgs,
                                       Long subOrganizationId,
                                       String subOrganizationName,
                                       boolean subOrganizationLocked) {}
