package com.afrodebab.cms.dto;

public record GoogleConnectionResponse(boolean configured, boolean connected, String email, boolean calendar, boolean sheets) {}
