package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;

/** OAuth authorization code from GitHub's redirect, exchanged server-side for an access token. */
public record GitHubConnectRequest(@NotBlank String code) {}
