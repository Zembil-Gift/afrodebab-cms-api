package com.afrodebab.cms.dto;

import com.afrodebab.cms.jpa.entity.GoogleSheetSync;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record GoogleSheetSyncRequest(@NotEmpty Set<GoogleSheetSync.Dataset> datasets) {}
