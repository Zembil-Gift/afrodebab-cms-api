package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Empty or null {@code subOrganizationIds} = everyone. Vice managers always send to their own branch. */
public record BroadcastRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10000) String body,
        List<Long> subOrganizationIds,
        boolean sendEmail
) {}
