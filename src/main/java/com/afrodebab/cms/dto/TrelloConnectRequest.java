package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;

/** Token returned by Trello's authorize flow, posted by the frontend to link the account. */
public record TrelloConnectRequest(@NotBlank String token) {}
