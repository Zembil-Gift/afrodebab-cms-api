package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The boards a manager chose to track. Replaces their previous selection wholesale. */
/** Each board carries the branches it credits; vice managers' boards always credit their own branch. */
public record TrelloBoardSelectionRequest(@NotNull List<TrelloBoardDto> boards) {}
