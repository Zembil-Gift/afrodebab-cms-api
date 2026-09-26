package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;

/** Authorization code from Google's redirect plus the exact redirect URI it was issued for. */
public record GoogleConnectRequest(@NotBlank String code, @NotBlank String redirectUri) {}
