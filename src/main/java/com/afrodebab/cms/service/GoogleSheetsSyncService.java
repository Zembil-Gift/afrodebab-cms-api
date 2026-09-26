package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.GoogleSheetSyncResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.EmployeeAttendance;
import com.afrodebab.cms.jpa.entity.EmployeeMetricScore;
import com.afrodebab.cms.jpa.entity.EmployeePayment;
import com.afrodebab.cms.jpa.entity.GoogleSheetSync;
import com.afrodebab.cms.jpa.entity.GoogleSheetSync.Dataset;
import com.afrodebab.cms.jpa.entity.JobApplication;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.EmployeeAttendanceRepository;
import com.afrodebab.cms.jpa.repository.EmployeeMetricScoreRepository;
import com.afrodebab.cms.jpa.repository.EmployeePaymentRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.GoogleSheetSyncRepository;
import com.afrodebab.cms.jpa.repository.JobApplicationRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One-way CMS → Google Sheets mirror. Each manager (or vice manager, limited to their branch)
 * gets one spreadsheet created by the app (drive.file scope, so we only ever touch files we
 * made), with one tab per chosen dataset. Every sync clears and rewrites those tabs, so edits
 * made in the sheet are overwritten and any drift fixes itself on the next run. Values are
 * written RAW so text from applicants can never run as a spreadsheet formula.
 */
@Service
public class GoogleSheetsSyncService {
    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsSyncService.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";
    // ponytail: rolling window keeps the attendance tab small; append-only rows if people need full history.
    private static final int ATTENDANCE_DAYS = 90;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<Dataset, String> TAB_TITLES = new EnumMap<>(Map.of(
            Dataset.EMPLOYEES, "Employees",
            Dataset.ATTENDANCE, "Attendance",
            Dataset.PAYMENTS, "Payroll",
            Dataset.METRICS, "Metrics",
            Dataset.JOB_APPLICATIONS, "Job applications"));

    private final GoogleSheetSyncRepository syncRepo;
    private final ManagerRepository managerRepo;
    private final EmployeeRepository employeeRepo;
    private final EmployeeAttendanceRepository attendanceRepo;
    private final EmployeePaymentRepository paymentRepo;
    private final EmployeeMetricScoreRepository metricRepo;
    private final JobApplicationRepository applicationRepo;
    private final OrganizationRepository organizationRepo;
    private final ManagerGoogleConnectionService googleConnection;
    private final GoogleApiClient google;
    private final TransactionTemplate txTemplate;

    public GoogleSheetsSyncService(GoogleSheetSyncRepository syncRepo,
                                   ManagerRepository managerRepo,
                                   EmployeeRepository employeeRepo,
                                   EmployeeAttendanceRepository attendanceRepo,
                                   EmployeePaymentRepository paymentRepo,
                                   EmployeeMetricScoreRepository metricRepo,
                                   JobApplicationRepository applicationRepo,
                                   OrganizationRepository organizationRepo,
                                   ManagerGoogleConnectionService googleConnection,
                                   GoogleApiClient google,
                                   PlatformTransactionManager txManager) {
        this.syncRepo = syncRepo;
        this.managerRepo = managerRepo;
        this.employeeRepo = employeeRepo;
        this.attendanceRepo = attendanceRepo;
        this.paymentRepo = paymentRepo;
        this.metricRepo = metricRepo;
        this.applicationRepo = applicationRepo;
        this.organizationRepo = organizationRepo;
        this.googleConnection = googleConnection;
        this.google = google;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    private record SyncRef(Long managerId, Long organizationId) {}

    // ---------------------------------------------------------------- manager-facing

    @Transactional(readOnly = true)
    public GoogleSheetSyncResponse get() {
        Manager manager = currentManager();
        return toResponse(manager, syncRepo.findByManagerId(manager.getId()).orElse(null));
    }

    /** Creates the spreadsheet on first use, saves the dataset choice, and syncs right away. */
    @Transactional
    public GoogleSheetSyncResponse enable(Set<Dataset> requested) {
        Manager manager = currentManager();
        Set<Dataset> datasets = EnumSet.copyOf(requested);
        if (isVice(manager) && datasets.remove(Dataset.JOB_APPLICATIONS) && datasets.isEmpty()) {
            throw new BadRequestException("Pick at least one dataset");
        }
        String token = sheetsToken(manager);
        GoogleSheetSync sync = syncRepo.findByManagerId(manager.getId()).orElseGet(() -> createSpreadsheet(manager, token));
        sync.setDatasets(datasets.stream().map(Enum::name).collect(Collectors.joining(",")));
        syncInto(manager, sync, token);
        return toResponse(manager, syncRepo.save(sync));
    }

    @Transactional
    public GoogleSheetSyncResponse syncNow() {
        Manager manager = currentManager();
        GoogleSheetSync sync = syncRepo.findByManagerId(manager.getId())
                .orElseThrow(() -> new NotFoundException("Google Sheets sync is not set up"));
        syncInto(manager, sync, sheetsToken(manager));
        return toResponse(manager, syncRepo.save(sync));
    }

    /** Stops syncing. The spreadsheet itself stays in the manager's Google Drive. */
    @Transactional
    public void disable() {
        syncRepo.findByManagerId(currentManager().getId()).ifPresent(syncRepo::delete);
    }

    // ---------------------------------------------------------------- scheduled

    // Not @Transactional: each sheet syncs in its own tenant-scoped transaction (see TrelloTrackerService).
    // ponytail: single-instance scheduler, same as the email ticker.
    @Scheduled(cron = "0 15 * * * *", zone = "UTC")
    public void syncAll() {
        List<SyncRef> refs = TenantContext.callAsRoot(() -> txTemplate.execute(status -> syncRepo.findAll().stream()
                .map(s -> new SyncRef(s.getManagerId(), s.getOrganizationId()))
                .toList()));
        if (refs == null) return;
        for (SyncRef ref : refs) {
            try {
                TenantContext.callAs(ref.organizationId(), () -> txTemplate.execute(status -> {
                    Manager manager = managerRepo.findById(ref.managerId()).orElse(null);
                    GoogleSheetSync sync = syncRepo.findByManagerId(ref.managerId()).orElse(null);
                    if (manager == null || sync == null || !manager.isActive()) return null;
                    try {
                        syncInto(manager, sync, sheetsToken(manager));
                    } catch (RuntimeException e) { // Google disconnected or token revoked
                        sync.setLastError(e.getMessage());
                    }
                    return syncRepo.save(sync);
                }));
            } catch (RuntimeException e) {
                log.error("Google Sheets sync failed for manager {} (org {})", ref.managerId(), ref.organizationId(), e);
            }
        }
    }

    // ---------------------------------------------------------------- Sheets API

    private GoogleSheetSync createSpreadsheet(Manager manager, String token) {
        String orgName = organizationRepo.findById(TenantContext.get()).map(o -> o.getName()).orElse("Company");
        String title = orgName + (isVice(manager) && manager.getSubOrganization() != null
                ? " - " + manager.getSubOrganization().getName() : "") + " (Mahberix)";
        JsonNode created = google.request("POST", SHEETS_API, token, Map.of("properties", Map.of("title", title)));
        GoogleSheetSync sync = new GoogleSheetSync();
        sync.setManagerId(manager.getId());
        sync.setSpreadsheetId(created.path("spreadsheetId").asText());
        sync.setSpreadsheetUrl(created.path("spreadsheetUrl").asText());
        return sync;
    }

    /** Rewrites every chosen tab; the outcome (time or error) is recorded on {@code sync} instead of thrown. */
    private void syncInto(Manager manager, GoogleSheetSync sync, String token) {
        try {
            List<Dataset> datasets = datasetsOf(sync);
            String base = SHEETS_API + "/" + sync.getSpreadsheetId();
            ensureTabs(base, token, datasets);

            List<String> ranges = datasets.stream().map(d -> quote(TAB_TITLES.get(d))).toList();
            google.request("POST", base + "/values:batchClear", token, Map.of("ranges", ranges));
            List<Map<String, Object>> data = datasets.stream()
                    .map(d -> Map.<String, Object>of("range", quote(TAB_TITLES.get(d)) + "!A1", "values", rows(d, manager)))
                    .toList();
            google.request("POST", base + "/values:batchUpdate", token, Map.of("valueInputOption", "RAW", "data", data));

            sync.setLastSyncedAt(Instant.now());
            sync.setLastError(null);
        } catch (ResponseStatusException e) {
            sync.setLastError(e.getStatusCode() == HttpStatus.NOT_FOUND
                    ? "The spreadsheet was deleted or you lost access to it. Turn the sync off and on to create a new one."
                    : e.getReason());
        } catch (RuntimeException e) {
            log.warn("Google Sheets sync failed for manager {}", manager.getId(), e);
            sync.setLastError("Sync failed: " + e.getMessage());
        }
    }

    private void ensureTabs(String base, String token, List<Dataset> datasets) {
        JsonNode meta = google.request("GET", base + "?fields=" + URLEncoder.encode("sheets.properties.title", StandardCharsets.UTF_8), token, null);
        Set<String> existing = new java.util.HashSet<>();
        meta.path("sheets").forEach(s -> existing.add(s.path("properties").path("title").asText()));
        List<Map<String, Object>> add = datasets.stream()
                .map(TAB_TITLES::get)
                .filter(title -> !existing.contains(title))
                .map(title -> Map.<String, Object>of("addSheet", Map.of("properties", Map.of("title", title))))
                .toList();
        if (!add.isEmpty()) google.request("POST", base + ":batchUpdate", token, Map.of("requests", add));
    }

    // ---------------------------------------------------------------- rows

    private List<List<?>> rows(Dataset dataset, Manager manager) {
        Predicate<Employee> inScope = inScope(manager);
        ZoneId zone = ZoneId.of(organizationRepo.findById(TenantContext.get()).map(o -> o.getEmailTimezone()).orElse("UTC"));
        return switch (dataset) {
            case EMPLOYEES -> table(
                    List.of("Name", "Email", "Phone", "Position", "Department", "Role", "Employment type", "Status",
                            "Branch", "Active", "Salary (ETB)", "Salary effective date", "Joined"),
                    employeeRepo.findAll(Sort.by("name")).stream().filter(inScope).map(e -> List.of(
                            e.getName(), e.getEmail(), text(e.getPhone()), text(e.getPosition()), text(e.getDepartment()),
                            text(e.getRole()), text(e.getEmploymentType()), text(e.getEmployeeStatus()), branch(e),
                            e.isActive() ? "Yes" : "No", money(e.getSalaryAmountMinor()), date(e.getSalaryEffectiveDate()),
                            e.getCreatedAt() == null ? "" : DATE_TIME.withZone(zone).format(e.getCreatedAt()))));
            case ATTENDANCE -> table(
                    List.of("Date", "Employee", "Email", "Branch", "Clock in", "Lunch out", "Lunch in", "Clock out", "Status", "Notes"),
                    attendanceRepo.findAllByAttendanceDateGreaterThanEqualOrderByAttendanceDateDesc(LocalDate.now(zone).minusDays(ATTENDANCE_DAYS))
                            .stream().filter(a -> inScope.test(a.getEmployee())).map(a -> List.of(
                                    date(a.getAttendanceDate()), a.getEmployee().getName(), a.getEmployee().getEmail(),
                                    branch(a.getEmployee()), time(a.getClockInAt(), zone), time(a.getLunchBreakInAt(), zone),
                                    time(a.getLunchBreakOutAt(), zone), time(a.getClockOutAt(), zone), status(a), text(a.getNotes()))));
            case PAYMENTS -> table(
                    List.of("Employee", "Email", "Branch", "Cycle start", "Due date", "Gross (ETB)", "Income tax (ETB)",
                            "Employee pension (ETB)", "Net (ETB)", "Paid (ETB)", "Status", "Paid at", "Reference"),
                    paymentRepo.findAll(Sort.by(Sort.Direction.DESC, "dueDate")).stream()
                            .filter(p -> inScope.test(p.getEmployee())).map(p -> List.of(
                                    p.getEmployee().getName(), p.getEmployee().getEmail(), branch(p.getEmployee()),
                                    date(p.getCycleStartDate()), date(p.getDueDate()), money(p.getGrossAmountMinor()),
                                    money(p.getIncomeTaxMinor()), money(p.getEmployeePensionMinor()), money(p.getAmountMinor()),
                                    money(p.getPaidAmountMinor()), p.getStatus().name(),
                                    p.getPaidAt() == null ? "" : DATE_TIME.withZone(zone).format(p.getPaidAt()),
                                    text(p.getTransactionReference()))));
            case METRICS -> table(
                    List.of("Employee", "Email", "Branch", "Period start", "Period end", "Leadership", "Attendance", "Tasks", "Overall"),
                    metricRepo.findAll(Sort.by(Sort.Direction.DESC, "periodStart")).stream()
                            .filter(m -> m.getEmployee() != null && inScope.test(m.getEmployee())).map(m -> List.of(
                                    m.getEmployee().getName(), m.getEmployee().getEmail(), branch(m.getEmployee()),
                                    date(m.getPeriodStart()), date(m.getPeriodEnd()), score(m.getLeadershipScore()),
                                    score(m.getAttendanceScore()), score(m.getTaskScore()), score(m.getOverallScore()))));
            case JOB_APPLICATIONS -> isVice(manager) ? List.of() : table(
                    List.of("Job", "Candidate", "Email", "Phone", "Status", "Applied"),
                    applicationRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(a -> List.of(
                            a.getJob().getTitle(), a.getFullName(), a.getEmail(), text(a.getPhoneNumber()),
                            a.getStatus() == null ? "" : a.getStatus().name(),
                            a.getCreatedAt() == null ? "" : DATE_TIME.withZone(zone).format(a.getCreatedAt()))));
        };
    }

    private static List<List<?>> table(List<?> header, Stream<? extends List<?>> rows) {
        List<List<?>> out = new ArrayList<>();
        out.add(header);
        rows.forEach(out::add);
        return out;
    }

    /** Vice managers only ever see their own branch. */
    private static Predicate<Employee> inScope(Manager manager) {
        if (!isVice(manager) || manager.getSubOrganization() == null) return e -> true;
        Long branchId = manager.getSubOrganization().getId();
        return e -> e.getSubOrganization() != null && branchId.equals(e.getSubOrganization().getId());
    }

    private static String branch(Employee e) {
        return e.getSubOrganization() == null ? "" : e.getSubOrganization().getName();
    }

    private static String status(EmployeeAttendance a) {
        return a.getAttendanceStatus() == null ? "" : a.getAttendanceStatus().entrySet().stream()
                .map(en -> en.getKey() + ": " + en.getValue()).collect(Collectors.joining(", "));
    }

    private static Object money(Long minor) {
        return minor == null ? "" : BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static Object score(BigDecimal value) {
        return value == null ? "" : value;
    }

    private static String date(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private static String time(Instant value, ZoneId zone) {
        return value == null ? "" : TIME.withZone(zone).format(value);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    // Tab titles contain spaces ("Job applications"), so A1 ranges need quoting.
    private static String quote(String title) {
        return "'" + title.replace("'", "''") + "'";
    }

    // ---------------------------------------------------------------- helpers

    private String sheetsToken(Manager manager) {
        return googleConnection.accessToken(manager, GoogleApiClient.SCOPE_SHEETS)
                .orElseThrow(() -> new BadRequestException("Connect Google and enable Sheets first"));
    }

    private static List<Dataset> datasetsOf(GoogleSheetSync sync) {
        return Arrays.stream(sync.getDatasets().split(","))
                .filter(s -> !s.isBlank())
                .map(Dataset::valueOf)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static boolean isVice(Manager manager) {
        return manager.getRole() == Manager.ManagerRole.VICE_MANAGER;
    }

    private Manager currentManager() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findWithSubOrganizationByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private GoogleSheetSyncResponse toResponse(Manager manager, GoogleSheetSync sync) {
        Collection<Dataset> available = isVice(manager)
                ? EnumSet.complementOf(EnumSet.of(Dataset.JOB_APPLICATIONS))
                : EnumSet.allOf(Dataset.class);
        return new GoogleSheetSyncResponse(
                sync != null,
                sync == null ? null : sync.getSpreadsheetUrl(),
                sync == null ? List.of() : datasetsOf(sync).stream().map(Enum::name).toList(),
                available.stream().map(Enum::name).toList(),
                sync == null ? null : sync.getLastSyncedAt(),
                sync == null ? null : sync.getLastError());
    }
}
