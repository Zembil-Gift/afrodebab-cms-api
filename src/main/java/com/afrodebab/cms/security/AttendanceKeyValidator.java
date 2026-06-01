package com.afrodebab.cms.security;

import com.afrodebab.cms.config.AttendancePolicyProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AttendanceKeyValidator {
    private final AttendancePolicyProperties attendancePolicyProperties;

    public AttendanceKeyValidator(AttendancePolicyProperties attendancePolicyProperties) {
        this.attendancePolicyProperties = attendancePolicyProperties;
    }

    public void requireValidKey(String providedKey) {
        String expectedKey = attendancePolicyProperties.getClientKey();
        if (expectedKey == null || expectedKey.isBlank()) {
            throw new IllegalStateException("Attendance client key is not configured");
        }
        if (providedKey == null || providedKey.isBlank() || !expectedKey.equals(providedKey)) {
            throw new AccessDeniedException("Invalid attendance key");
        }
    }
}
