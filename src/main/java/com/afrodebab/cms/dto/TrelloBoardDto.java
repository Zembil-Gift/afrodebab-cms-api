package com.afrodebab.cms.dto;

import java.util.List;

/** A Trello board (id + name), used both for the available list and the saved selection. */
/** subOrganizationIds: branches this board credits (empty = all); null in the available list. */
public record TrelloBoardDto(String id, String name, List<Long> subOrganizationIds) {}
