package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.TrelloActivityResponse;
import com.afrodebab.cms.dto.TrelloAggregateResponse;
import com.afrodebab.cms.dto.TrelloReportResponse;
import com.afrodebab.cms.jpa.entity.TrelloActivity;
import com.afrodebab.cms.jpa.repository.TrelloActivityRepository;
import com.afrodebab.cms.service.TrelloTrackerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin - Trello Tracker")
@RestController
@RequestMapping("/admin/trello")
public class AdminTrelloTrackerController {

    private final TrelloTrackerService trackerService;
    private final TrelloActivityRepository activityRepo;

    public AdminTrelloTrackerController(TrelloTrackerService trackerService,
                                        TrelloActivityRepository activityRepo) {
        this.trackerService = trackerService;
        this.activityRepo = activityRepo;
    }

    @PostMapping("/sync")
    public Map<String, Object> manualSync() {
        int count = trackerService.syncActivities();
        return Map.of(
                "status", "success",
                "message", "Trello activity sync triggered manually",
                "syncedCount", count
        );
    }

    @GetMapping("/activities")
    public Page<TrelloActivityResponse> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityTimestamp"));
        return activityRepo.findAllByOrderByActivityTimestampDesc(pageable)
                .map(this::toResponse);
    }

    @GetMapping("/activities/employee/{employeeId}")
    public Page<TrelloActivityResponse> getEmployeeActivities(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityTimestamp"));
        return activityRepo.findAllByEmployeeIdOrderByActivityTimestampDesc(employeeId, pageable)
                .map(this::toResponse);
    }

    @GetMapping("/report/{employeeId}")
    public TrelloReportResponse getEmployeeReport(@PathVariable Long employeeId) {
        return trackerService.getEmployeeReport(employeeId);
    }

    @GetMapping("/aggregate")
    public List<TrelloAggregateResponse> getAggregateResults(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "WEEKLY") String periodType
    ) {
        return trackerService.getAggregatedResults(employeeId, periodType);
    }

    private TrelloActivityResponse toResponse(TrelloActivity act) {
        return trackerService.toActivityResponse(act);
    }
}
