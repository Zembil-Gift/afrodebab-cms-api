package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An interviewer: an org manager, an employee, or someone invited by email only. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewParticipant {
    public enum Kind { MANAGER, EMPLOYEE, EXTERNAL }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "employee_id")
    private Long employeeId;

    private String name;

    @Column(nullable = false)
    private String email;
}
