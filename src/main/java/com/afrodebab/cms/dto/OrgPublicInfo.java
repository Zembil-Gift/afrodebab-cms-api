package com.afrodebab.cms.dto;

/** Minimal, public-safe description of an organization, keyed by its URL slug. */
public record OrgPublicInfo(String name, String slug, String plan) {}
