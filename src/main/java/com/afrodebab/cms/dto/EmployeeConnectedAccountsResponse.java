package com.afrodebab.cms.dto;

public record EmployeeConnectedAccountsResponse(
        Long employeeId,
        String employeeName,
        String githubUsername,
        String trelloUsername,
        String telegramUsername
) {}
