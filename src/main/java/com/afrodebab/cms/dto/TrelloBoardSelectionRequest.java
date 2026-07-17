package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The boards a manager chose to track. Replaces their previous selection wholesale. */
public record TrelloBoardSelectionRequest(@NotNull List<TrelloBoardDto> boards) {}
