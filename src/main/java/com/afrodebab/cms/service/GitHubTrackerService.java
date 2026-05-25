package com.afrodebab.cms.service;

import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.GitHubActivity;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.GitHubActivityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.afrodebab.cms.dto.GitHubActivityResponse;
import com.afrodebab.cms.dto.GitHubReportResponse;
import com.afrodebab.cms.dto.GitHubAggregateResponse;
import com.afrodebab.cms.exception.NotFoundException;

@Service
public class GitHubTrackerService {
    private static final Logger log = LoggerFactory.getLogger(GitHubTrackerService.class);

    private final GitHubActivityRepository activityRepo;
    private final EmployeeRepository employeeRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${GITHUB_KEY:}")
    private String githubKey;

    @Value("${GITHUB_ORG_NAME:}")
    private String githubOrgName;

    @Autowired
    public GitHubTrackerService(GitHubActivityRepository activityRepo,
                                EmployeeRepository employeeRepo,
                                ObjectMapper objectMapper) {
        this(activityRepo, employeeRepo, objectMapper, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public GitHubTrackerService(GitHubActivityRepository activityRepo,
                                EmployeeRepository employeeRepo,
                                ObjectMapper objectMapper,
                                HttpClient httpClient) {
        this.activityRepo = activityRepo;
        this.employeeRepo = employeeRepo;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Scheduled(cron = "0 0 0 * * MON") // sync once a week (Monday 00:00)
    public void scheduleSync() {
        log.info("Starting scheduled GitHub activity synchronization...");
        try {
            syncActivities();
        } catch (Exception e) {
            log.error("Error during scheduled GitHub activity synchronization", e);
        }
    }

    @Transactional
    public int syncActivities() {
        if (githubKey == null || githubKey.isBlank() || githubOrgName == null || githubOrgName.isBlank()) {
            log.warn("GitHub tracker skipped: GITHUB_KEY or GITHUB_ORG_NAME environment variables are not set");
            return 0;
        }

        // Fetch active employees with configured github usernames
        List<Employee> employees = employeeRepo.findAllByActiveTrueOrderByNameAsc();
        Map<String, Employee> usernameToEmployee = new HashMap<>();
        for (Employee emp : employees) {
            if (emp.getGithubUsername() != null && !emp.getGithubUsername().isBlank()) {
                usernameToEmployee.put(emp.getGithubUsername().toLowerCase(Locale.ROOT).trim(), emp);
            }
        }

        if (usernameToEmployee.isEmpty()) {
            log.info("No active employees with github_username configured. Skipping sync.");
            return 0;
        }

        log.info("Syncing GitHub activities for org: {}, tracking {} mapped employees", githubOrgName, usernameToEmployee.size());
        
        int totalSaved = 0;
        int page = 1;
        boolean caughtUp = false;
        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // GitHub API allows max 10 pages for events
        while (page <= 10 && !caughtUp) {
            try {
                List<JsonNode> events = fetchEventsPage(page);
                if (events == null || events.isEmpty()) {
                    break;
                }

                log.debug("Fetched {} events from page {} of org {}", events.size(), page, githubOrgName);

                List<GitHubActivity> activitiesToSave = new ArrayList<>();

                for (JsonNode event : events) {
                    String eventId = event.path("id").asText("");
                    if (eventId.isBlank()) continue;

                    String createdAtStr = event.path("created_at").asText("");
                    if (createdAtStr.isBlank()) continue;
                    Instant eventTimestamp = Instant.parse(createdAtStr);

                    // Stop if we hit events older than 90 days
                    if (eventTimestamp.isBefore(ninetyDaysAgo)) {
                        caughtUp = true;
                        break;
                    }

                    // Check if we already have this main event in DB
                    // Note: Since individual commits have distinct IDs (commit_{sha}),
                    // and PR events have distinct action IDs (pr_opened_{pr.id}),
                    // we will check their custom composite IDs later.
                    // For general checking, if we see the base event ID exists, and it's not a PushEvent,
                    // we can safely consider it synced.
                    String eventType = event.path("type").asText("");
                    boolean isPush = "PushEvent".equalsIgnoreCase(eventType);

                    if (!isPush && activityRepo.existsByActivityId(eventId)) {
                        caughtUp = true;
                        break;
                    }

                    String actorLogin = event.path("actor").path("login").asText("").toLowerCase(Locale.ROOT).trim();
                    Employee employee = usernameToEmployee.get(actorLogin);

                    if (employee != null) {
                        List<GitHubActivity> parsed = parseEvent(event, employee, eventId, eventTimestamp);
                        for (GitHubActivity act : parsed) {
                            if (!activityRepo.existsByActivityId(act.getActivityId())) {
                                activitiesToSave.add(act);
                            } else if (isPush) {
                                // If a commit already exists, it doesn't mean we stop sync (since other commits in the same PushEvent or earlier events might not be synced),
                                // but we skip adding duplicates.
                                // However, if we hit an existing commit, we can also consider this a signal that we're catching up.
                                // To be safe, we don't immediately terminate on a single commit unless all commits in the push are duplicates.
                            }
                        }
                    }
                }

                if (!activitiesToSave.isEmpty()) {
                    activityRepo.saveAll(activitiesToSave);
                    totalSaved += activitiesToSave.size();
                    log.info("Saved {} new GitHub activities from page {}", activitiesToSave.size(), page);
                }

                // If no new activities were found on this page and we are not in initial sync, 
                // we might have already synced everything. We can safely stop to avoid rate limits.
                if (activitiesToSave.isEmpty() && page > 1) {
                    // Check if page has events. If we mapped no employees, activitiesToSave is empty.
                    // But if we hit events that are already in DB, caughtUp is set to true.
                }

                page++;
            } catch (Exception e) {
                log.error("Failed to sync GitHub events on page " + page, e);
                break;
            }
        }

        log.info("Finished GitHub activity sync. Total activities saved: {}", totalSaved);
        return totalSaved;
    }

    private List<JsonNode> fetchEventsPage(int page) throws Exception {
        String url = String.format("https://api.github.com/orgs/%s/events?page=%d&per_page=30", githubOrgName, page);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + githubKey)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AfroDebab-CMS-API")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                root.forEach(list::add);
                return list;
            }
        } else {
            log.error("GitHub Events API returned non-200 status: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("GitHub API error: " + response.statusCode());
        }

        return Collections.emptyList();
    }

    private List<GitHubActivity> parseEvent(JsonNode event, Employee employee, String eventId, Instant eventTimestamp) {
        List<GitHubActivity> activities = new ArrayList<>();
        String eventType = event.path("type").asText("");
        String repoName = event.path("repo").path("name").asText("unknown/repo");
        String repoUrl = "https://github.com/" + repoName;

        switch (eventType) {
            case "PushEvent":
                JsonNode commits = event.path("payload").path("commits");
                if (commits.isArray() && !commits.isEmpty()) {
                    int count = commits.size();
                    for (JsonNode commit : commits) {
                        String sha = commit.path("sha").asText("");
                        if (sha.isBlank()) continue;

                        String message = commit.path("message").asText("");
                        // Limit title to first line of commit message
                        String firstLine = message.split("\n")[0];
                        if (firstLine.length() > 200) {
                            firstLine = firstLine.substring(0, 197) + "...";
                        }

                        GitHubActivity act = GitHubActivity.builder()
                                .employee(employee)
                                .githubUsername(employee.getGithubUsername())
                                .activityType("COMMIT")
                                .repository(repoName)
                                .activityId("commit_" + sha)
                                .title(firstLine)
                                .description(String.format("Pushed commit: %s\nFull Message: %s", sha.substring(0, 8), message))
                                .url(repoUrl + "/commit/" + sha)
                                .activityTimestamp(eventTimestamp)
                                .build();
                        activities.add(act);
                    }
                }
                break;

            case "PullRequestEvent":
                JsonNode prPayload = event.path("payload");
                String action = prPayload.path("action").asText("");
                JsonNode pr = prPayload.path("pull_request");
                String prId = pr.path("id").asText("");
                String prTitle = pr.path("title").asText("Untitled Pull Request");
                String prUrl = pr.path("html_url").asText(repoUrl + "/pulls");
                String prDescription = pr.path("body").asText("");

                if (!prId.isBlank() && ("opened".equalsIgnoreCase(action) || "closed".equalsIgnoreCase(action))) {
                    boolean merged = pr.path("merged").asBoolean(false);
                    String activityType = "PR_OPENED";
                    String actId = "pr_opened_" + prId;
                    String activityTitle = "Opened PR: " + prTitle;

                    if ("closed".equalsIgnoreCase(action)) {
                        if (merged) {
                            activityType = "PR_MERGED";
                            actId = "pr_merged_" + prId;
                            activityTitle = "Merged PR: " + prTitle;
                        } else {
                            activityType = "PR_CLOSED";
                            actId = "pr_closed_" + prId;
                            activityTitle = "Closed PR (Unmerged): " + prTitle;
                        }
                    }

                    GitHubActivity act = GitHubActivity.builder()
                            .employee(employee)
                            .githubUsername(employee.getGithubUsername())
                            .activityType(activityType)
                            .repository(repoName)
                            .activityId(actId)
                            .title(activityTitle)
                            .description(prDescription)
                            .url(prUrl)
                            .activityTimestamp(eventTimestamp)
                            .build();
                    activities.add(act);
                }
                break;

            case "PullRequestReviewEvent":
                JsonNode reviewPayload = event.path("payload");
                JsonNode review = reviewPayload.path("review");
                String reviewId = review.path("id").asText("");
                JsonNode revPr = reviewPayload.path("pull_request");
                String revPrTitle = revPr.path("title").asText("Pull Request");
                String reviewUrl = review.path("html_url").asText(repoUrl + "/pulls");
                String reviewState = review.path("state").asText("submitted");

                if (!reviewId.isBlank()) {
                    GitHubActivity act = GitHubActivity.builder()
                            .employee(employee)
                            .githubUsername(employee.getGithubUsername())
                            .activityType("PR_REVIEW")
                            .repository(repoName)
                            .activityId("pr_review_" + reviewId)
                            .title(String.format("Reviewed PR (%s): %s", reviewState.toUpperCase(), revPrTitle))
                            .description(review.path("body").asText(""))
                            .url(reviewUrl)
                            .activityTimestamp(eventTimestamp)
                            .build();
                    activities.add(act);
                }
                break;

            case "IssuesEvent":
                JsonNode issuePayload = event.path("payload");
                String issueAction = issuePayload.path("action").asText("");
                JsonNode issue = issuePayload.path("issue");
                String issueId = issue.path("id").asText("");
                String issueTitle = issue.path("title").asText("Untitled Issue");
                String issueUrl = issue.path("html_url").asText(repoUrl + "/issues");

                if (!issueId.isBlank() && ("opened".equalsIgnoreCase(issueAction) || "closed".equalsIgnoreCase(issueAction))) {
                    String activityType = "opened".equalsIgnoreCase(issueAction) ? "ISSUE_OPENED" : "ISSUE_CLOSED";
                    String activityTitle = ("opened".equalsIgnoreCase(issueAction) ? "Opened Issue: " : "Closed Issue: ") + issueTitle;

                    GitHubActivity act = GitHubActivity.builder()
                            .employee(employee)
                            .githubUsername(employee.getGithubUsername())
                            .activityType(activityType)
                            .repository(repoName)
                            .activityId("issue_" + issueAction + "_" + issueId)
                            .title(activityTitle)
                            .description(issue.path("body").asText(""))
                            .url(issueUrl)
                            .activityTimestamp(eventTimestamp)
                            .build();
                    activities.add(act);
                }
                break;

            default:
                // Skip untracked event types
                break;
        }

        return activities;
    }

    @Transactional(readOnly = true)
    public GitHubReportResponse getEmployeeReport(Long employeeId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        List<GitHubActivity> allActivities = activityRepo.findAllByEmployeeId(employeeId);

        long totalCommits = 0;
        long prsOpened = 0;
        long prsMerged = 0;
        long prsClosed = 0;
        long prReviews = 0;
        long issuesOpened = 0;
        long issuesClosed = 0;

        for (GitHubActivity act : allActivities) {
            switch (act.getActivityType()) {
                case "COMMIT":
                    totalCommits++;
                    break;
                case "PR_OPENED":
                    prsOpened++;
                    break;
                case "PR_MERGED":
                    prsMerged++;
                    break;
                case "PR_CLOSED":
                    prsClosed++;
                    break;
                case "PR_REVIEW":
                    prReviews++;
                    break;
                case "ISSUE_OPENED":
                    issuesOpened++;
                    break;
                case "ISSUE_CLOSED":
                    issuesClosed++;
                    break;
            }
        }

        List<GitHubActivityResponse> recentActivities = allActivities.stream()
                .sorted(Comparator.comparing(GitHubActivity::getActivityTimestamp).reversed())
                .limit(10)
                .map(this::toActivityResponse)
                .toList();

        return new GitHubReportResponse(
                employee.getId(),
                employee.getName(),
                employee.getGithubUsername(),
                totalCommits,
                prsOpened,
                prsMerged,
                prsClosed,
                prReviews,
                issuesOpened,
                issuesClosed,
                recentActivities
        );
    }

    @Transactional(readOnly = true)
    public List<GitHubAggregateResponse> getAggregatedResults(Long employeeId, String periodType) {
        if (employeeId != null && !employeeRepo.existsById(employeeId)) {
            throw new NotFoundException("Employee not found with ID: " + employeeId);
        }

        List<GitHubActivity> activities = (employeeId == null)
                ? activityRepo.findAll()
                : activityRepo.findAllByEmployeeId(employeeId);

        Map<String, List<GitHubActivity>> grouped = new HashMap<>();
        for (GitHubActivity act : activities) {
            Instant timestamp = act.getActivityTimestamp();
            if (timestamp == null) continue;

            LocalDate date = LocalDate.ofInstant(timestamp, ZoneOffset.UTC);
            String periodKey;
            if ("MONTHLY".equalsIgnoreCase(periodType)) {
                periodKey = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            } else {
                LocalDate monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                periodKey = monday.toString();
            }

            grouped.computeIfAbsent(periodKey, k -> new ArrayList<>()).add(act);
        }

        List<GitHubAggregateResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<GitHubActivity>> entry : grouped.entrySet()) {
            String period = entry.getKey();
            List<GitHubActivity> periodActs = entry.getValue();

            long commits = 0;
            long prsOpened = 0;
            long prsMerged = 0;
            long prsClosed = 0;
            long prReviews = 0;
            long issuesOpened = 0;
            long issuesClosed = 0;

            for (GitHubActivity act : periodActs) {
                switch (act.getActivityType()) {
                    case "COMMIT":
                        commits++;
                        break;
                    case "PR_OPENED":
                        prsOpened++;
                        break;
                    case "PR_MERGED":
                        prsMerged++;
                        break;
                    case "PR_CLOSED":
                        prsClosed++;
                        break;
                    case "PR_REVIEW":
                        prReviews++;
                        break;
                    case "ISSUE_OPENED":
                        issuesOpened++;
                        break;
                    case "ISSUE_CLOSED":
                        issuesClosed++;
                        break;
                }
            }

            result.add(new GitHubAggregateResponse(
                    period,
                    commits,
                    prsOpened,
                    prsMerged,
                    prsClosed,
                    prReviews,
                    issuesOpened,
                    issuesClosed
            ));
        }

        result.sort(Comparator.comparing(GitHubAggregateResponse::period).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public GitHubReportResponse getEmployeeReportByEmail(String email) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        return getEmployeeReport(employee.getId());
    }

    @Transactional(readOnly = true)
    public List<GitHubAggregateResponse> getAggregatedResultsByEmail(String email, String periodType) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        return getAggregatedResults(employee.getId(), periodType);
    }

    public GitHubActivityResponse toActivityResponse(GitHubActivity act) {
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
