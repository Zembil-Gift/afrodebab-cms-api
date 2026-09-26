package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The GitHub orgs a manager chose to track. Replaces their previous selection wholesale. */
public record GitHubOrgSelectionRequest(@NotNull List<GitHubOrgDto> orgs) {}
