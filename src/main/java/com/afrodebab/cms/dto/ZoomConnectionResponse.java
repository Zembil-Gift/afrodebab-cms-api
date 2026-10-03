package com.afrodebab.cms.dto;

/** {@code configured} is false when the server has no ZOOM_CLIENT_ID/SECRET. */
public record ZoomConnectionResponse(boolean configured, boolean connected, String email) {}
