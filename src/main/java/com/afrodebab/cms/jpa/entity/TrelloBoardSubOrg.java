package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One sub-organization a tracked Trello board credits; none for a board = all of them. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrelloBoardSubOrg {

    @Column(name = "board_id", nullable = false)
    private String boardId;

    @Column(name = "sub_organization_id", nullable = false)
    private Long subOrganizationId;
}
