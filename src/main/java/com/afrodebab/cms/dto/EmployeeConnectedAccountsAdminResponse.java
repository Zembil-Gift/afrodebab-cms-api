package com.afrodebab.cms.dto;

public record EmployeeConnectedAccountsAdminResponse(
        Long employeeId,
        String employeeName,
        String employeeEmail,
        String githubUsername,
        String trelloUsername,
        String telegramUsername
) {}
