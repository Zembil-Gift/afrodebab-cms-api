package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;

/** Authorization code from the OAuth provider's (Google, Zoom) redirect plus the exact redirect URI it was issued for. */
public record GoogleConnectRequest(@NotBlank String code, @NotBlank String redirectUri) {}
