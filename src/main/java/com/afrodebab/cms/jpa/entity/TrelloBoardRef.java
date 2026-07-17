package com.afrodebab.cms.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One Trello board a manager chose to track (id + display name). */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrelloBoardRef {

    @Column(name = "board_id", nullable = false)
    private String boardId;

    @Column(name = "board_name")
    private String boardName;
}
