package com.afrodebab.cms.dto;

import java.util.List;

/** Current manager's Trello connection status + the boards they selected to track. */
public record TrelloConnectionResponse(boolean connected, List<TrelloBoardDto> selectedBoards) {}
