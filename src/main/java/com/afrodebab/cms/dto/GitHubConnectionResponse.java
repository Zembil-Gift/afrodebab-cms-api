package com.afrodebab.cms.dto;

import java.util.List;

/** Current manager's GitHub connection status + the orgs they selected to track. */
public record GitHubConnectionResponse(boolean connected, List<GitHubOrgDto> selectedOrgs) {}
