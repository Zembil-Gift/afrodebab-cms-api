package com.afrodebab.cms.dto;

import java.util.List;

/** A GitHub organization (login handle + display name), for the available list and selection. */
/** subOrganizationIds: branches this org credits (empty = all); null in the available list. */
public record GitHubOrgDto(String login, String name, List<Long> subOrganizationIds) {}
