package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.GitHubActivityResponse;
import com.afrodebab.cms.jpa.entity.GitHubActivity;
import com.afrodebab.cms.jpa.repository.GitHubActivityRepository;
import com.afrodebab.cms.service.GitHubTrackerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import com.afrodebab.cms.dto.GitHubAggregateResponse;
import com.afrodebab.cms.dto.GitHubReportResponse;

@Tag(name = "Admin - GitHub Tracker")
@RestController
@RequestMapping("/admin/github")
public class AdminGitHubTrackerController {

    private final GitHubTrackerService trackerService;
    private final GitHubActivityRepository activityRepo;

    public AdminGitHubTrackerController(GitHubTrackerService trackerService,
                                        GitHubActivityRepository activityRepo) {
        this.trackerService = trackerService;
        this.activityRepo = activityRepo;
    }

    @PostMapping("/sync")
    public Map<String, Object> manualSync() {
        int count = trackerService.syncActivities();
        return Map.of(
                "status", "success",
                "message", "GitHub activity sync triggered manually",
                "syncedCount", count
        );
    }

    @GetMapping("/activities")
    public Page<GitHubActivityResponse> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityTimestamp"));
        return activityRepo.findAllByOrderByActivityTimestampDesc(pageable)
                .map(this::toResponse);
    }

    @GetMapping("/activities/employee/{employeeId}")
    public Page<GitHubActivityResponse> getEmployeeActivities(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityTimestamp"));
        return activityRepo.findAllByEmployeeIdOrderByActivityTimestampDesc(employeeId, pageable)
                .map(this::toResponse);
    }

    @GetMapping("/report/{employeeId}")
    public GitHubReportResponse getEmployeeReport(@PathVariable Long employeeId) {
        return trackerService.getEmployeeReport(employeeId);
    }

    @GetMapping("/aggregate")
    public List<GitHubAggregateResponse> getAggregateResults(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "WEEKLY") String periodType
    ) {
        return trackerService.getAggregatedResults(employeeId, periodType);
    }

    private GitHubActivityResponse toResponse(GitHubActivity act) {
        return new GitHubActivityResponse(
                act.getId(),
                act.getEmployee().getId(),
                act.getEmployee().getName(),
                act.getGithubUsername(),
                act.getActivityType(),
                act.getRepository(),
                act.getActivityId(),
                act.getTitle(),
                act.getDescription(),
                act.getUrl(),
                act.getActivityTimestamp(),
                act.getCreatedAt(),
                act.getUpdatedAt()
        );
    }
}
