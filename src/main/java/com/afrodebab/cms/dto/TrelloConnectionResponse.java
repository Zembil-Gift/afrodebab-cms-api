package com.afrodebab.cms.dto;

import java.util.List;

/** Current manager's Trello connection status + the boards they selected to track. */
/**
 * {@code subOrganizationLocked} is true for vice managers, whose boards always credit their own
 * branch (subOrganizationId/Name); managers pick branches per board instead (both null).
 */
public record TrelloConnectionResponse(boolean connected,
                                       List<TrelloBoardDto> selectedBoards,
                                       Long subOrganizationId,
                                       String subOrganizationName,
                                       boolean subOrganizationLocked) {}
