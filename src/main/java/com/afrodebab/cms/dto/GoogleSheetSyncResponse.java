package com.afrodebab.cms.dto;

import java.time.Instant;
import java.util.List;

/** {@code enabled} false = no spreadsheet yet. {@code availableDatasets} excludes org-only data for vice managers. */
public record GoogleSheetSyncResponse(
        boolean enabled,
        String spreadsheetUrl,
        List<String> datasets,
        List<String> availableDatasets,
        Instant lastSyncedAt,
        String lastError
) {}
