package com.afrodebab.cms.dto;

/** A GitHub organization (login handle + display name), for the available list and selection. */
public record GitHubOrgDto(String login, String name) {}
