package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One GitHub organization a manager chose to track (login handle + display name). */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubOrgRef {

    @Column(name = "org_login", nullable = false)
    private String orgLogin;

    @Column(name = "org_name")
    private String orgName;
}
