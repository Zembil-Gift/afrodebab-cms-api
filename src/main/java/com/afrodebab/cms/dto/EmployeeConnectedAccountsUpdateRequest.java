package com.afrodebab.cms.dto;

public record EmployeeConnectedAccountsUpdateRequest(
        String githubUsername,
        String trelloUsername,
        String telegramUsername
) {}
