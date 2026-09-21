package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.TrelloActivityResponse;
import com.afrodebab.cms.dto.TrelloAggregateResponse;
import com.afrodebab.cms.dto.TrelloReportResponse;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.TrelloBoardSubOrg;
import com.afrodebab.cms.jpa.entity.TrelloActivity;
import com.afrodebab.cms.jpa.entity.TrelloBoardRef;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.TrelloActivityRepository;
import com.afrodebab.cms.security.TokenCipher;
import com.afrodebab.cms.service.ManagerTrelloConnectionService.ManagerRef;
import com.afrodebab.cms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TrelloTrackerService {
    private static final Logger log = LoggerFactory.getLogger(TrelloTrackerService.class);
    private static final List<String> ACTION_FILTERS = List.of(
            "createCard",
            "updateCard",
            "commentCard",
            "updateCheckItemStateOnCard",
            "addAttachmentToCard"
    );

    private final TrelloActivityRepository activityRepo;
    private final EmployeeRepository employeeRepo;
    private final ManagerRepository managerRepo;
    private final ManagerTrelloConnectionService connectionService;
    private final TokenCipher cipher;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TransactionTemplate txTemplate;

    // Shared app key (identifies our Trello app). Each manager's personal token is stored
    // encrypted on their Manager record; that pair (key + token) authorizes every API call.
    @Value("${TRELLO_API:}")
    private String trelloKey;

    @Autowired
    public TrelloTrackerService(TrelloActivityRepository activityRepo,
                                EmployeeRepository employeeRepo,
                                ManagerRepository managerRepo,
                                ManagerTrelloConnectionService connectionService,
                                TokenCipher cipher,
                                ObjectMapper objectMapper,
                                PlatformTransactionManager txManager) {
        this(activityRepo, employeeRepo, managerRepo, connectionService, cipher, objectMapper,
                txManager, HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build());
    }

    public TrelloTrackerService(TrelloActivityRepository activityRepo,
                                EmployeeRepository employeeRepo,
                                ManagerRepository managerRepo,
                                ManagerTrelloConnectionService connectionService,
                                TokenCipher cipher,
                                ObjectMapper objectMapper,
                                PlatformTransactionManager txManager,
                                HttpClient httpClient) {
        this.activityRepo = activityRepo;
        this.employeeRepo = employeeRepo;
        this.managerRepo = managerRepo;
        this.connectionService = connectionService;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
        this.txTemplate = new TransactionTemplate(txManager);
        this.httpClient = httpClient;
    }

    @Scheduled(cron = "0 30 0 * * MON") // sync once a week (Monday 00:30)
    public void scheduleSync() {
        log.info("Starting scheduled Trello activity synchronization...");
        try {
            syncActivities();
        } catch (Exception e) {
            log.error("Error during scheduled Trello activity synchronization", e);
        }
    }

    /**
     * Orchestrates the sync across every organization: finds all managers who have connected
     * Trello (root scope), then for each runs the sync inside a fresh transaction scoped to
     * that manager's org, so activity rows are attributed to the right tenant. Not itself
     * transactional — each org gets its own tenant-scoped session via {@link TransactionTemplate}.
     */
    /**
     * Syncs only the logged-in (vice) manager's own connection, in the tenant already set from
     * their JWT. Used by vice managers, who must not trigger other branches' or orgs' syncs.
     */
    public int syncCurrentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Long managerId = managerRepo.findByEmailIgnoreCase(email)
                .map(Manager::getId)
                .orElseThrow(() -> new NotFoundException("Manager not found"));
        if (!cipher.isConfigured() || trelloKey == null || trelloKey.isBlank()) return 0;
        Integer saved = txTemplate.execute(status -> syncManager(managerId));
        return saved == null ? 0 : saved;
    }

    public int syncActivities() {
        if (trelloKey == null || trelloKey.isBlank()) {
            log.warn("Trello tracker skipped: TRELLO_API (app key) is not set");
            return 0;
        }
        if (!cipher.isConfigured()) {
            log.warn("Trello tracker skipped: app.security.token-encryption-key is not set");
            return 0;
        }

        List<ManagerRef> managers = TenantContext.callAsRoot(connectionService::findConnectedManagers);
        if (managers.isEmpty()) {
            log.info("No managers have connected Trello. Skipping sync.");
            return 0;
        }

        int totalSaved = 0;
        for (ManagerRef ref : managers) {
            try {
                totalSaved += TenantContext.callAs(ref.organizationId(),
                        () -> txTemplate.execute(status -> syncManager(ref.managerId())));
            } catch (Exception e) {
                log.error("Failed to sync Trello for manager {} (org {})",
                        ref.managerId(), ref.organizationId(), e);
            }
        }

        log.info("Finished Trello activity sync. Total activities saved: {}", totalSaved);
        return totalSaved;
    }

    /** Trello username → employee, limited to the given branches (empty = all branches). */
    private static Map<String, Employee> byUsername(List<Employee> employees, Set<Long> branches) {
        Map<String, Employee> byUsername = new HashMap<>();
        for (Employee emp : employees) {
            String username = emp.getTrelloUsername();
            if (username == null || username.isBlank()) continue;
            if (!branches.isEmpty() && (emp.getSubOrganization() == null
                    || !branches.contains(emp.getSubOrganization().getId()))) continue;
            byUsername.put(username.toLowerCase(Locale.ROOT).trim(), emp);
        }
        return byUsername;
    }

    /** Runs inside a transaction already scoped to the manager's org (tenant filtering applies). */
    private int syncManager(Long managerId) {
        Manager manager = managerRepo.findById(managerId).orElse(null);
        if (manager == null || manager.getTrelloToken() == null) return 0;

        Set<TrelloBoardRef> boards = manager.getTrelloBoards();
        if (boards.isEmpty()) return 0;

        // Vice managers always credit their own branch; a manager's boards each credit the
        // branches ticked for them (none ticked = every branch).
        Long viceBranch = manager.getRole() == Manager.ManagerRole.VICE_MANAGER && manager.getSubOrganization() != null
                ? manager.getSubOrganization().getId() : null;
        Map<String, Set<Long>> branchesByBoard = new HashMap<>();
        for (TrelloBoardSubOrg scope : manager.getTrelloBoardSubOrgs()) {
            branchesByBoard.computeIfAbsent(scope.getBoardId(), k -> new HashSet<>()).add(scope.getSubOrganizationId());
        }
        List<Employee> employees = employeeRepo.findAllByActiveTrueOrderByNameAsc();

        String token = cipher.decrypt(manager.getTrelloToken());
        int saved = 0;
        for (TrelloBoardRef board : boards) {
            Set<Long> branches = viceBranch != null
                    ? Set.of(viceBranch)
                    : branchesByBoard.getOrDefault(board.getBoardId(), Set.of());
            Map<String, Employee> usernameToEmployee = byUsername(employees, branches);
            if (usernameToEmployee.isEmpty()) continue;
            saved += syncBoard(board.getBoardId(), board.getBoardName(), token, usernameToEmployee);
        }
        return saved;
    }

    // ponytail: within an org, existsByActivityId dedupes across managers who track the same
    // board. Two *different* orgs tracking the same board would collide on the global unique
    // activity_id — rare (separate tenants sharing one Trello board); make activity_id unique
    // per-org if that ever happens.
    private int syncBoard(String boardId, String boardName, String token,
                          Map<String, Employee> usernameToEmployee) {
        if (boardId == null || boardId.isBlank()) return 0;

        int totalSaved = 0;
        int page = 1;
        boolean caughtUp = false;
        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);
        String before = null;

        while (page <= 10 && !caughtUp) {
            try {
                List<JsonNode> actions = fetchActionsPage(boardId, before, token);
                if (actions == null || actions.isEmpty()) {
                    break;
                }

                List<TrelloActivity> activitiesToSave = new ArrayList<>();
                for (JsonNode action : actions) {
                    String actionId = action.path("id").asText("");
                    if (actionId.isBlank()) continue;

                    String dateStr = action.path("date").asText("");
                    if (dateStr.isBlank()) continue;
                    Instant actionTimestamp = Instant.parse(dateStr);

                    if (actionTimestamp.isBefore(ninetyDaysAgo)) {
                        caughtUp = true;
                        break;
                    }

                    if (activityRepo.existsByActivityId(actionId)) {
                        caughtUp = true;
                        break;
                    }

                    String actor = action.path("memberCreator").path("username").asText("")
                            .toLowerCase(Locale.ROOT).trim();
                    Employee employee = usernameToEmployee.get(actor);
                    if (employee == null) continue;

                    TrelloActivity parsed = parseAction(action, employee, boardName);
                    if (parsed != null && !activityRepo.existsByActivityId(parsed.getActivityId())) {
                        activitiesToSave.add(parsed);
                    }
                }

                if (!activitiesToSave.isEmpty()) {
                    activityRepo.saveAll(activitiesToSave);
                    totalSaved += activitiesToSave.size();
                    log.info("Saved {} new Trello activities from board {} page {}",
                            activitiesToSave.size(), boardName, page);
                }

                before = actions.get(actions.size() - 1).path("id").asText(null);
                if (before == null) break;
                page++;
            } catch (Exception e) {
                log.error("Failed to sync Trello actions for board " + boardName + " on page " + page, e);
                break;
            }
        }
        return totalSaved;
    }

    private List<JsonNode> fetchActionsPage(String boardId, String before, String token) throws Exception {
        String filter = String.join(",", ACTION_FILTERS);
        StringBuilder url = new StringBuilder("https://api.trello.com/1/boards/")
                .append(encode(boardId))
                .append("/actions?filter=")
                .append(encode(filter))
                .append("&limit=200")
                .append("&key=").append(encode(trelloKey))
                .append("&token=").append(encode(token));
        if (before != null) {
            url.append("&before=").append(encode(before));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("Accept", "application/json")
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
            log.error("Trello actions API returned non-200 status: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Trello API error: " + response.statusCode());
        }

        return Collections.emptyList();
    }

    private TrelloActivity parseAction(JsonNode action, Employee employee, String defaultBoardName) {
        String type = action.path("type").asText("");
        JsonNode data = action.path("data");
        String boardName = data.path("board").path("name").asText(defaultBoardName);
        JsonNode card = data.path("card");
        String cardId = card.path("id").asText("");
        if (cardId.isBlank()) return null;
        String cardName = card.path("name").asText("Untitled Card");
        String cardUrl = card.path("url").asText("");
        if (cardUrl.isBlank()) {
            String shortLink = card.path("shortLink").asText("");
            if (!shortLink.isBlank()) {
                cardUrl = "https://trello.com/c/" + shortLink;
            }
        }

        String activityType = null;
        String description = null;
        String listName = null;

        switch (type) {
            case "createCard":
                activityType = "CARD_CREATED";
                listName = data.path("list").path("name").asText(null);
                if (listName != null) {
                    description = "Created card in list: " + listName;
                }
                break;
            case "updateCard":
                String listBefore = data.path("listBefore").path("name").asText(null);
                String listAfter = data.path("listAfter").path("name").asText(null);
                if (listBefore != null && listAfter != null) {
                    activityType = "CARD_MOVED";
                    listName = listAfter;
                    description = "Moved card from " + listBefore + " to " + listAfter;
                    break;
                }
                boolean oldClosed = data.path("old").path("closed").asBoolean(false);
                boolean newClosed = data.path("card").path("closed").asBoolean(false);
                if (!oldClosed && newClosed) {
                    activityType = "CARD_ARCHIVED";
                    description = "Archived card";
                }
                break;
            case "commentCard":
                activityType = "COMMENT_ADDED";
                description = data.path("text").asText("");
                break;
            case "updateCheckItemStateOnCard":
                String state = data.path("checkItem").path("state").asText("");
                String checkName = data.path("checkItem").path("name").asText("");
                if ("complete".equalsIgnoreCase(state)) {
                    activityType = "CHECKITEM_COMPLETED";
                    description = checkName.isBlank() ? "Completed checklist item" : "Completed: " + checkName;
                } else {
                    activityType = "CHECKITEM_INCOMPLETE";
                    description = checkName.isBlank() ? "Unchecked checklist item" : "Unchecked: " + checkName;
                }
                break;
            case "addAttachmentToCard":
                activityType = "ATTACHMENT_ADDED";
                String attachmentName = data.path("attachment").path("name").asText("");
                description = attachmentName.isBlank() ? "Added attachment" : "Added attachment: " + attachmentName;
                break;
            default:
                return null;
        }

        if (activityType == null) return null;

        String actionId = action.path("id").asText("");
        Instant actionTimestamp = Instant.parse(action.path("date").asText(""));

        return TrelloActivity.builder()
                .employee(employee)
                .trelloUsername(employee.getTrelloUsername())
                .activityType(activityType)
                .boardName(boardName)
                .cardId(cardId)
                .cardName(cardName)
                .listName(listName)
                .activityId(actionId)
                .description(description)
                .url(cardUrl)
                .activityTimestamp(actionTimestamp)
                .build();
    }

    @Transactional(readOnly = true)
    public TrelloReportResponse getEmployeeReport(Long employeeId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        List<TrelloActivity> allActivities = activityRepo.findAllByEmployeeId(employeeId);

        long cardsCreated = 0;
        long cardsMoved = 0;
        long cardsArchived = 0;
        long commentsAdded = 0;
        long checkItemsCompleted = 0;
        long attachmentsAdded = 0;

        for (TrelloActivity act : allActivities) {
            switch (act.getActivityType()) {
                case "CARD_CREATED":
                    cardsCreated++;
                    break;
                case "CARD_MOVED":
                    cardsMoved++;
                    break;
                case "CARD_ARCHIVED":
                    cardsArchived++;
                    break;
                case "COMMENT_ADDED":
                    commentsAdded++;
                    break;
                case "CHECKITEM_COMPLETED":
                    checkItemsCompleted++;
                    break;
                case "ATTACHMENT_ADDED":
                    attachmentsAdded++;
                    break;
            }
        }

        List<TrelloActivityResponse> recentActivities = allActivities.stream()
                .sorted(Comparator.comparing(TrelloActivity::getActivityTimestamp).reversed())
                .limit(10)
                .map(this::toActivityResponse)
                .toList();

        return new TrelloReportResponse(
                employee.getId(),
                employee.getName(),
                employee.getTrelloUsername(),
                cardsCreated,
                cardsMoved,
                cardsArchived,
                commentsAdded,
                checkItemsCompleted,
                attachmentsAdded,
                recentActivities
        );
    }

    @Transactional(readOnly = true)
    public List<TrelloAggregateResponse> getAggregatedResults(Long employeeId, String periodType) {
        if (employeeId != null && !employeeRepo.existsById(employeeId)) {
            throw new NotFoundException("Employee not found with ID: " + employeeId);
        }

        List<TrelloActivity> activities = (employeeId == null)
                ? activityRepo.findAll()
                : activityRepo.findAllByEmployeeId(employeeId);

        Map<String, List<TrelloActivity>> grouped = new HashMap<>();
        for (TrelloActivity act : activities) {
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

        List<TrelloAggregateResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<TrelloActivity>> entry : grouped.entrySet()) {
            String period = entry.getKey();
            List<TrelloActivity> periodActs = entry.getValue();

            long cardsCreated = 0;
            long cardsMoved = 0;
            long cardsArchived = 0;
            long commentsAdded = 0;
            long checkItemsCompleted = 0;
            long attachmentsAdded = 0;

            for (TrelloActivity act : periodActs) {
                switch (act.getActivityType()) {
                    case "CARD_CREATED":
                        cardsCreated++;
                        break;
                    case "CARD_MOVED":
                        cardsMoved++;
                        break;
                    case "CARD_ARCHIVED":
                        cardsArchived++;
                        break;
                    case "COMMENT_ADDED":
                        commentsAdded++;
                        break;
                    case "CHECKITEM_COMPLETED":
                        checkItemsCompleted++;
                        break;
                    case "ATTACHMENT_ADDED":
                        attachmentsAdded++;
                        break;
                }
            }

            result.add(new TrelloAggregateResponse(
                    period,
                    cardsCreated,
                    cardsMoved,
                    cardsArchived,
                    commentsAdded,
                    checkItemsCompleted,
                    attachmentsAdded
            ));
        }

        result.sort(Comparator.comparing(TrelloAggregateResponse::period).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public TrelloReportResponse getEmployeeReportByEmail(String email) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        return getEmployeeReport(employee.getId());
    }

    public TrelloReportResponse getEmployeeReportByEmailOrFallback(String email) {
        try {
            return getEmployeeReportByEmail(email);
        } catch (Exception e) {
            log.error("Failed to fetch trello report for {}: {}", email, e.getMessage());
            return new TrelloReportResponse(null, null, null, 0, 0, 0, 0, 0, 0, Collections.emptyList());
        }
    }

    @Transactional(readOnly = true)
    public List<TrelloAggregateResponse> getAggregatedResultsByEmail(String email, String periodType) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found with email: " + email));
        return getAggregatedResults(employee.getId(), periodType);
    }

    public TrelloActivityResponse toActivityResponse(TrelloActivity act) {
        return new TrelloActivityResponse(
                act.getId(),
                act.getEmployee().getId(),
                act.getEmployee().getName(),
                act.getTrelloUsername(),
                act.getActivityType(),
                act.getBoardName(),
                act.getCardId(),
                act.getCardName(),
                act.getListName(),
                act.getActivityId(),
                act.getDescription(),
                act.getUrl(),
                act.getActivityTimestamp(),
                act.getCreatedAt(),
                act.getUpdatedAt()
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
