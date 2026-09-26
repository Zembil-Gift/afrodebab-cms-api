package com.afrodebab.cms.dto;

/** {@code emailHtml} is the full email as recipients get it; {@code bodyHtml} is the in-app notification body. */
public record BroadcastPreviewResponse(String subject, String emailHtml, String bodyHtml, int recipientCount) {}
