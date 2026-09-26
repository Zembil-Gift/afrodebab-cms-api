package com.afrodebab.cms.dto;

/** A Trello board (id + name), used both for the available list and the saved selection. */
public record TrelloBoardDto(String id, String name) {}
