package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.GoogleSheetSyncRequest;
import com.afrodebab.cms.dto.GoogleSheetSyncResponse;
import com.afrodebab.cms.service.GoogleSheetsSyncService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** The caller's own spreadsheet sync; vice managers get branch-only data (enforced in the service). */
@Tag(name = "Manager - Google Sheets Sync")
@RestController
@RequestMapping({"/manager/google/sheets", "/vice-manager/google/sheets"})
public class GoogleSheetsSyncController {

    private final GoogleSheetsSyncService service;

    public GoogleSheetsSyncController(GoogleSheetsSyncService service) {
        this.service = service;
    }

    @GetMapping
    public GoogleSheetSyncResponse get() {
        return service.get();
    }

    @PutMapping
    public GoogleSheetSyncResponse enable(@Valid @RequestBody GoogleSheetSyncRequest req) {
        return service.enable(req.datasets());
    }

    @PostMapping("/sync")
    public GoogleSheetSyncResponse syncNow() {
        return service.syncNow();
    }

    @DeleteMapping
    public void disable() {
        service.disable();
    }
}
