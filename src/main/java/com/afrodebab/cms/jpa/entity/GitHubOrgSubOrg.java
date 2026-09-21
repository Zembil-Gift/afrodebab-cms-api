package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One sub-organization a tracked GitHub organization credits; none for an org = all of them. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubOrgSubOrg {

    @Column(name = "org_login", nullable = false)
    private String orgLogin;

    @Column(name = "sub_organization_id", nullable = false)
    private Long subOrganizationId;
}
