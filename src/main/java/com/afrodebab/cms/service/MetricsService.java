package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmployeeMetricSummaryResponse;
import com.afrodebab.cms.dto.TelegramSupportReportResponse;
import com.afrodebab.cms.dto.TrelloReportResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.EmployeeAttendance;
import com.afrodebab.cms.jpa.entity.EmployeeMetricScore;
import com.afrodebab.cms.jpa.entity.PeerReview;
import com.afrodebab.cms.jpa.repository.EmployeeAttendanceRepository;
import com.afrodebab.cms.jpa.repository.EmployeeMetricScoreRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.PeerReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MetricsService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeAttendanceRepository employeeAttendanceRepository;
    private final PeerReviewRepository peerReviewRepository;
    private final EmployeeMetricScoreRepository employeeMetricScoreRepository;
    private final TrelloTrackerService trelloTrackerService;
    private final TelegramSupportTrackerService telegramSupportTrackerService;

    public MetricsService(EmployeeRepository employeeRepository,
                          EmployeeAttendanceRepository employeeAttendanceRepository,
                          PeerReviewRepository peerReviewRepository,
                          EmployeeMetricScoreRepository employeeMetricScoreRepository,
                          TrelloTrackerService trelloTrackerService,
                          TelegramSupportTrackerService telegramSupportTrackerService) {
        this.employeeRepository = employeeRepository;
        this.employeeAttendanceRepository = employeeAttendanceRepository;
        this.peerReviewRepository = peerReviewRepository;
        this.employeeMetricScoreRepository = employeeMetricScoreRepository;
        this.trelloTrackerService = trelloTrackerService;
        this.telegramSupportTrackerService = telegramSupportTrackerService;
    }

    @Transactional(readOnly = true)
    public EmployeeMetricSummaryResponse getOwnMetrics(String employeeEmail,
                                                       LocalDate periodStart,
                                                       LocalDate periodEnd,
                                                       boolean persistSnapshot) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return getEmployeeMetrics(employee.getId(), periodStart, periodEnd, persistSnapshot);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeMetricSummaryResponse> getEmployeeMetricsPage(LocalDate periodStart,
                                                                      LocalDate periodEnd,
                                                                      String department,
                                                                      String role,
                                                                      Pageable pageable,
                                                                      boolean persistSnapshot) {
        validatePeriod(periodStart, periodEnd);
        return employeeRepository.findAllByDepartmentAndRole(department, role, pageable)
                .map(employee -> computeAndOptionallyPersist(employee, periodStart, periodEnd, persistSnapshot));
    }

    @Transactional(readOnly = true)
    public EmployeeMetricSummaryResponse getEmployeeMetrics(Long employeeId,
                                                            LocalDate periodStart,
                                                            LocalDate periodEnd,
                                                            boolean persistSnapshot) {
        validatePeriod(periodStart, periodEnd);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return computeAndOptionallyPersist(employee, periodStart, periodEnd, persistSnapshot);
    }

    @Transactional
    public EmployeeMetricSummaryResponse refreshSnapshot(Long employeeId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return computeAndOptionallyPersist(employee, periodStart, periodEnd, true);
    }

    private EmployeeMetricSummaryResponse computeAndOptionallyPersist(Employee employee,
                                                                      LocalDate periodStart,
                                                                      LocalDate periodEnd,
                                                                      boolean persistSnapshot) {
        BigDecimal leadershipScore = computeLeadershipScore(employee.getId(), periodStart, periodEnd);
        BigDecimal attendanceScore = computeAttendanceScore(employee, periodStart, periodEnd);
        BigDecimal taskScore = computeTaskScore(employee, periodStart, periodEnd);
        BigDecimal supportScore = computeSupportScore(employee, periodStart, periodEnd);
        BigDecimal overallScore = computeOverallScore(employee.getRole(), leadershipScore, attendanceScore, taskScore, supportScore);

        String strengthSummary = computeStrengthSummary(leadershipScore, attendanceScore, taskScore, supportScore);
        String improvementSummary = computeImprovementSummary(leadershipScore, attendanceScore, taskScore, supportScore);

        if (persistSnapshot) {
            EmployeeMetricScore snapshot = new EmployeeMetricScore();
            snapshot.setEmployee(employee);
            snapshot.setPeriodStart(periodStart);
            snapshot.setPeriodEnd(periodEnd);
            snapshot.setLeadershipScore(leadershipScore);
            snapshot.setAttendanceScore(attendanceScore);
            snapshot.setTaskScore(taskScore);
            snapshot.setSupportScore(supportScore);
            snapshot.setOverallScore(overallScore);
            snapshot.setStrengthSummary(strengthSummary);
            snapshot.setImprovementSummary(improvementSummary);
            employeeMetricScoreRepository.save(snapshot);
        }

        return new EmployeeMetricSummaryResponse(
                employee.getId(),
                employee.getName(),
                employee.getRole(),
                employee.getDepartment(),
                employee.getEmploymentType(),
                employee.getEmployeeStatus(),
                periodStart,
                periodEnd,
                leadershipScore,
                attendanceScore,
                taskScore,
                supportScore,
                overallScore,
                strengthSummary,
                improvementSummary
        );
    }

    private BigDecimal computeLeadershipScore(Long employeeId, LocalDate periodStart, LocalDate periodEnd) {
        List<PeerReview> rows = peerReviewRepository.findAllByRevieweeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                employeeId,
                periodStart,
                periodEnd
        );
        if (rows.isEmpty()) {
            return null;
        }

        int totalPoints = 0;
        for (PeerReview row : rows) {
            if (row.getRating() == null) {
                continue;
            }
            totalPoints += switch (row.getRating()) {
                case EXCEEDS_THE_BAR -> 3;
                case MEETS_THE_BAR -> 2;
                case NEEDS_IMPROVEMENT -> 1;
            };
        }
        int maxPoints = rows.size() * 3;
        return percentage(totalPoints, maxPoints);
    }

    private BigDecimal computeAttendanceScore(Employee employee, LocalDate periodStart, LocalDate periodEnd) {
        Set<DayOfWeek> scheduleDays = employee.getOfficeDays();
        if (scheduleDays == null || scheduleDays.isEmpty()) {
            return null;
        }

        List<EmployeeAttendance> rows = employeeAttendanceRepository
                .findAllByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        employee.getId(),
                        periodStart,
                        periodEnd
                );
        Map<LocalDate, EmployeeAttendance> byDate = new HashMap<>();
        for (EmployeeAttendance row : rows) {
            byDate.put(row.getAttendanceDate(), row);
        }

        int countedDays = 0;
        int totalPoints = 0;
        for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            if (!scheduleDays.contains(date.getDayOfWeek())) {
                continue;
            }

            EmployeeAttendance row = byDate.get(date);
            String finalStatus = extractFinalAttendanceStatus(row);
            if ("APPROVED_LEAVE".equals(finalStatus)) {
                continue;
            }

            countedDays++;
            totalPoints += switch (finalStatus) {
                case "ON_TIME", "REMOTE_APPROVED" -> 100;
                case "LATE" -> 70;
                default -> 0;
            };
        }

        if (countedDays == 0) {
            return null;
        }
        return percentage(totalPoints, countedDays * 100);
    }

    private String extractFinalAttendanceStatus(EmployeeAttendance attendance) {
        if (attendance == null || attendance.getAttendanceStatus() == null) {
            return "ABSENT";
        }
        String value = attendance.getAttendanceStatus().get("final");
        if (value == null || value.isBlank()) {
            return "ABSENT";
        }
        return value;
    }

    private BigDecimal computeTaskScore(Employee employee, LocalDate periodStart, LocalDate periodEnd) {
        TrelloReportResponse report = trelloTrackerService.getEmployeeReportByEmailOrFallback(employee.getEmail());
        return BigDecimal.valueOf(report.checkItemsCompleted());
    }

    private BigDecimal computeSupportScore(Employee employee, LocalDate periodStart, LocalDate periodEnd) {
        TelegramSupportReportResponse report = telegramSupportTrackerService.getEmployeeReportByEmail(
                employee.getEmail(), null, null, periodStart.toString(), periodEnd.toString()
        );
        return BigDecimal.valueOf(report.totals().resolved());
    }

    private BigDecimal computeOverallScore(String role,
                                           BigDecimal leadershipScore,
                                           BigDecimal attendanceScore,
                                           BigDecimal taskScore,
                                           BigDecimal supportScore) {
        Map<String, BigDecimal> scores = new HashMap<>();
        scores.put("leadership", leadershipScore);
        scores.put("attendance", attendanceScore);
        scores.put("task", taskScore);
        scores.put("support", supportScore);
        scores.put("team", null);

        Map<String, Integer> weights = resolveWeights(role);

        BigDecimal weightedSum = BigDecimal.ZERO;
        int activeWeight = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            BigDecimal score = scores.get(entry.getKey());
            if (score == null) {
                continue;
            }
            int weight = entry.getValue();
            weightedSum = weightedSum.add(score.multiply(BigDecimal.valueOf(weight)));
            activeWeight += weight;
        }

        if (activeWeight == 0) {
            return null;
        }
        return weightedSum
                .divide(BigDecimal.valueOf(activeWeight), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Integer> resolveWeights(String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        Map<String, Integer> weights = new HashMap<>();
        if ("DEVELOPER".equals(normalizedRole)) {
            weights.put("leadership", 25);
            weights.put("attendance", 15);
            weights.put("task", 50);
            weights.put("team", 10);
            return weights;
        }
        if ("CUSTOMER_SUPPORT".equals(normalizedRole)) {
            weights.put("leadership", 25);
            weights.put("attendance", 20);
            weights.put("support", 45);
            weights.put("team", 10);
            return weights;
        }
        weights.put("leadership", 30);
        weights.put("attendance", 20);
        weights.put("task", 40);
        weights.put("team", 10);
        return weights;
    }

    private String computeStrengthSummary(BigDecimal leadership,
                                          BigDecimal attendance,
                                          BigDecimal task,
                                          BigDecimal support) {
        Map<String, BigDecimal> components = componentScores(leadership, attendance, task, support);
        if (components.isEmpty()) {
            return null;
        }
        String maxLabel = null;
        BigDecimal maxScore = null;
        for (Map.Entry<String, BigDecimal> entry : components.entrySet()) {
            if (maxScore == null || entry.getValue().compareTo(maxScore) > 0) {
                maxScore = entry.getValue();
                maxLabel = entry.getKey();
            }
        }
        return maxLabel == null ? null : "Strongest area: " + maxLabel + " (" + maxScore + "%)";
    }

    private String computeImprovementSummary(BigDecimal leadership,
                                             BigDecimal attendance,
                                             BigDecimal task,
                                             BigDecimal support) {
        Map<String, BigDecimal> components = componentScores(leadership, attendance, task, support);
        if (components.size() < 2) {
            return null;
        }
        String minLabel = null;
        BigDecimal minScore = null;
        for (Map.Entry<String, BigDecimal> entry : components.entrySet()) {
            if (minScore == null || entry.getValue().compareTo(minScore) < 0) {
                minScore = entry.getValue();
                minLabel = entry.getKey();
            }
        }
        return minLabel == null ? null : "Needs improvement: " + minLabel + " (" + minScore + "%)";
    }

    private Map<String, BigDecimal> componentScores(BigDecimal leadership,
                                                    BigDecimal attendance,
                                                    BigDecimal task,
                                                    BigDecimal support) {
        Map<String, BigDecimal> scores = new HashMap<>();
        if (leadership != null) scores.put("Leadership", leadership);
        if (attendance != null) scores.put("Attendance", attendance);
        if (task != null) scores.put("Task", task);
        if (support != null) scores.put("Support", support);
        return scores;
    }

    private BigDecimal percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new BadRequestException("periodStart and periodEnd are required");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new BadRequestException("periodEnd must be on or after periodStart");
        }
    }
}
