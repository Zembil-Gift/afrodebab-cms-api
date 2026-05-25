package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.dto.PeerReviewAvailableEmployeeResponse;
import com.afrodebab.cms.dto.PeerReviewEmployeeResultsResponse;
import com.afrodebab.cms.dto.PeerReviewEmployeeSummaryResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodCreateRequest;
import com.afrodebab.cms.dto.PeerReviewPeriodResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodResultsResponse;
import com.afrodebab.cms.dto.PeerReviewPeriodStatusResponse;
import com.afrodebab.cms.dto.PeerReviewPrincipleAverageResponse;
import com.afrodebab.cms.dto.PeerReviewRatingInput;
import com.afrodebab.cms.dto.PeerReviewResponse;
import com.afrodebab.cms.dto.PeerReviewSelfResultsResponse;
import com.afrodebab.cms.dto.PeerReviewSubmitRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.LeadershipPrinciple;
import com.afrodebab.cms.jpa.entity.PeerReview;
import com.afrodebab.cms.jpa.entity.PeerReviewPeriod;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.LeadershipPrincipleRepository;
import com.afrodebab.cms.jpa.repository.PeerReviewPeriodRepository;
import com.afrodebab.cms.jpa.repository.PeerReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PeerReviewService {
    private final PeerReviewRepository peerReviewRepository;
    private final LeadershipPrincipleRepository leadershipPrincipleRepository;
    private final EmployeeRepository employeeRepository;
    private final PeerReviewPeriodRepository peerReviewPeriodRepository;

    public PeerReviewService(PeerReviewRepository peerReviewRepository,
                             LeadershipPrincipleRepository leadershipPrincipleRepository,
                             EmployeeRepository employeeRepository,
                             PeerReviewPeriodRepository peerReviewPeriodRepository) {
        this.peerReviewRepository = peerReviewRepository;
        this.leadershipPrincipleRepository = leadershipPrincipleRepository;
        this.employeeRepository = employeeRepository;
        this.peerReviewPeriodRepository = peerReviewPeriodRepository;
    }

    @Transactional(readOnly = true)
    public List<LeadershipPrincipleResponse> listActivePrinciples() {
        return leadershipPrincipleRepository.findAllByActiveTrueOrderByIdAsc()
                .stream()
                .map(this::toPrincipleResponse)
                .toList();
    }

    @Transactional
    public List<PeerReviewResponse> submit(String reviewerEmail, PeerReviewSubmitRequest request) {
        validatePeriod(request.periodStart(), request.periodEnd());
        ensureInitiatedPeriod(request.periodStart(), request.periodEnd());

        Employee reviewer = employeeRepository.findByEmailIgnoreCase(reviewerEmail)
                .orElseThrow(() -> new NotFoundException("Reviewer employee not found"));
        Employee reviewee = employeeRepository.findById(request.revieweeId())
                .orElseThrow(() -> new NotFoundException("Reviewee employee not found"));

        if (reviewer.getId().equals(reviewee.getId())) {
            throw new BadRequestException("Self reviews are not allowed");
        }

        Set<Long> principleIds = new HashSet<>();
        for (PeerReviewRatingInput ratingInput : request.ratings()) {
            if (!principleIds.add(ratingInput.principleId())) {
                throw new BadRequestException("Duplicate principleId in ratings: " + ratingInput.principleId());
            }
        }

        List<LeadershipPrinciple> principles = leadershipPrincipleRepository.findAllById(principleIds);
        Map<Long, LeadershipPrinciple> principleMap = principles.stream()
                .collect(Collectors.toMap(LeadershipPrinciple::getId, p -> p));
        if (principleMap.size() != principleIds.size()) {
            throw new BadRequestException("One or more principleIds are invalid");
        }

        Map<Long, PeerReview> savedByPrincipleId = new HashMap<>();
        for (PeerReviewRatingInput ratingInput : request.ratings()) {
            var existing = peerReviewRepository.findByReviewerIdAndRevieweeIdAndPrincipleIdAndPeriodStartAndPeriodEnd(
                    reviewer.getId(),
                    reviewee.getId(),
                    ratingInput.principleId(),
                    request.periodStart(),
                    request.periodEnd()
            );

            // One-time submission: once a rating is set, it cannot be updated.
            if (existing.isPresent() && existing.get().getRating() != null) {
                throw new BadRequestException("Peer review already submitted for principleId: " + ratingInput.principleId());
            }

            PeerReview review = existing.orElseGet(PeerReview::new);
            review.setReviewer(reviewer);
            review.setReviewee(reviewee);
            review.setPeriodStart(request.periodStart());
            review.setPeriodEnd(request.periodEnd());
            review.setPrinciple(principleMap.get(ratingInput.principleId()));
            review.setRating(ratingInput.rating());
            review.setComment(ratingInput.comment());

            savedByPrincipleId.put(ratingInput.principleId(), peerReviewRepository.save(review));
        }

        return savedByPrincipleId.values().stream()
                .sorted((a, b) -> Long.compare(a.getPrinciple().getId(), b.getPrinciple().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PeerReviewPeriodResponse initiatePeriod(PeerReviewPeriodCreateRequest request) {
        validatePeriod(request.periodStart(), request.periodEnd());

        return peerReviewPeriodRepository.findByPeriodStartAndPeriodEnd(request.periodStart(), request.periodEnd())
                .map(this::toPeriodResponse)
                .orElseGet(() -> {
                    String name = normalizePeriodName(request.name());
                    peerReviewPeriodRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
                        throw new BadRequestException("Peer review period name already exists");
                    });

                    PeerReviewPeriod period = new PeerReviewPeriod();
                    period.setPeriodStart(request.periodStart());
                    period.setPeriodEnd(request.periodEnd());
                    period.setName(name);
                    return toPeriodResponse(peerReviewPeriodRepository.save(period));
                });
    }

    @Transactional(readOnly = true)
    public List<PeerReviewPeriodResponse> listInitiatedPeriods() {
        return peerReviewPeriodRepository.findAllByOrderByPeriodStartDescPeriodEndDesc()
                .stream()
                .map(this::toPeriodResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeerReviewPeriodStatusResponse> listInitiatedPeriodsWithSubmissionStatus(String employeeEmail) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        List<PeerReviewPeriod> periods = peerReviewPeriodRepository.findAllByOrderByPeriodStartDescPeriodEndDesc();
        if (periods.isEmpty()) {
            return List.of();
        }

        long activePrincipleCount = leadershipPrincipleRepository.countByActiveTrue();
        List<PeerReview> reviews = peerReviewRepository.findAllByReviewerId(employee.getId());
        Set<PeriodKey> periodKeys = periods.stream()
                .map(period -> new PeriodKey(period.getPeriodStart(), period.getPeriodEnd()))
                .collect(Collectors.toSet());

        Map<PeriodKey, Map<Long, Set<Long>>> revieweePrinciplesByPeriod = new HashMap<>();
        for (PeerReview review : reviews) {
            PeriodKey key = new PeriodKey(review.getPeriodStart(), review.getPeriodEnd());
            if (!periodKeys.contains(key)) {
                continue;
            }
            revieweePrinciplesByPeriod
                    .computeIfAbsent(key, ignored -> new HashMap<>())
                    .computeIfAbsent(review.getReviewee().getId(), ignored -> new HashSet<>())
                    .add(review.getPrinciple().getId());
        }

        return periods.stream()
                .map(period -> {
                    PeriodKey key = new PeriodKey(period.getPeriodStart(), period.getPeriodEnd());
                    boolean submitted = false;
                    if (activePrincipleCount > 0) {
                        Map<Long, Set<Long>> byReviewee = revieweePrinciplesByPeriod.get(key);
                        if (byReviewee != null) {
                            submitted = byReviewee.values().stream()
                                    .anyMatch(principles -> principles.size() >= activePrincipleCount);
                        }
                    }
                    return new PeerReviewPeriodStatusResponse(
                            period.getId(),
                            period.getName(),
                            period.getPeriodStart(),
                            period.getPeriodEnd(),
                            submitted
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PeerReviewPeriodResultsResponse getPeriodResults(Long periodId) {
        PeerReviewPeriod period = peerReviewPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Peer review period not found"));
        validatePeriod(period.getPeriodStart(), period.getPeriodEnd());

        List<PeerReview> reviews = peerReviewRepository.findAllByPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                period.getPeriodStart(),
                period.getPeriodEnd()
        );
        Map<Long, EmployeeAggregate> aggregates = buildAggregates(reviews);

        List<Employee> employees = resolveEmployeesForAdmin(reviews);
        List<PeerReviewEmployeeResultsResponse> results = employees.stream()
                .map(employee -> toEmployeeResults(employee, aggregates, listActivePrincipleEntities()))
                .toList();

        return new PeerReviewPeriodResultsResponse(
                period.getId(),
                period.getName(),
                period.getPeriodStart(),
                period.getPeriodEnd(),
                results
        );
    }

    @Transactional(readOnly = true)
    public PeerReviewSelfResultsResponse getSelfPeriodResults(String employeeEmail, Long periodId) {
        PeerReviewPeriod period = peerReviewPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Peer review period not found"));
        validatePeriod(period.getPeriodStart(), period.getPeriodEnd());

        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        List<PeerReview> reviews = peerReviewRepository.findAllByRevieweeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                employee.getId(),
                period.getPeriodStart(),
                period.getPeriodEnd()
        );
        Map<Long, EmployeeAggregate> aggregates = buildAggregates(reviews);

        PeerReviewEmployeeResultsResponse employeeResults = toEmployeeResults(employee, aggregates, listActivePrincipleEntities());

        return new PeerReviewSelfResultsResponse(
                period.getId(),
                period.getName(),
                period.getPeriodStart(),
                period.getPeriodEnd(),
                employeeResults
        );
    }

    @Transactional(readOnly = true)
    public List<PeerReviewAvailableEmployeeResponse> listAvailableEmployeesForEmployee(String employeeEmail) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        return employeeRepository.findAllByActiveTrueAndIdNotOrderByNameAsc(employee.getId())
                .stream()
                .map(this::toAvailableEmployeeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeerReviewAvailableEmployeeResponse> listAvailableEmployeesForAdmin(String adminEmail) {
        Long excludeId = null;
        if (adminEmail != null && !adminEmail.isBlank()) {
            excludeId = employeeRepository.findByEmailIgnoreCase(adminEmail)
                    .map(Employee::getId)
                    .orElse(null);
        }

        List<Employee> employees = excludeId == null
                ? employeeRepository.findAllByActiveTrueOrderByNameAsc()
                : employeeRepository.findAllByActiveTrueAndIdNotOrderByNameAsc(excludeId);

        return employees.stream()
                .map(this::toAvailableEmployeeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeerReviewResponse> listByPeriod(LocalDate periodStart, LocalDate periodEnd, Long revieweeId) {
        validatePeriod(periodStart, periodEnd);

        List<PeerReview> rows;
        if (revieweeId != null) {
            rows = peerReviewRepository.findAllByRevieweeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                    revieweeId,
                    periodStart,
                    periodEnd
            );
        } else {
            rows = peerReviewRepository.findAllByPeriodStartAndPeriodEndOrderByCreatedAtDesc(periodStart, periodEnd);
        }

        return rows.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeerReviewEmployeeSummaryResponse> summarizeByEmployee(LocalDate periodStart,
                                                                      LocalDate periodEnd,
                                                                      Long revieweeId) {
        validatePeriod(periodStart, periodEnd);

        if (revieweeId != null) {
            Employee employee = employeeRepository.findById(revieweeId)
                    .orElseThrow(() -> new NotFoundException("Employee not found"));

            List<PeerReview> reviews = peerReviewRepository.findAllByRevieweeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                    revieweeId,
                    periodStart,
                    periodEnd
            );
            Map<Long, EmployeeAggregate> aggregates = buildAggregates(reviews);

            return List.of(toEmployeeSummary(employee, periodStart, periodEnd, aggregates.get(employee.getId())));
        }

        List<PeerReview> reviews = peerReviewRepository.findAllByPeriodStartAndPeriodEndOrderByCreatedAtDesc(periodStart, periodEnd);
        Map<Long, EmployeeAggregate> aggregates = buildAggregates(reviews);

        return resolveEmployeesForAdmin(reviews)
                .stream()
                .map(employee -> toEmployeeSummary(employee, periodStart, periodEnd, aggregates.get(employee.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeerReviewResponse> listOwnReceived(String employeeEmail, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);

        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        return peerReviewRepository.findAllByRevieweeIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
                        employee.getId(),
                        periodStart,
                        periodEnd
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LeadershipPrincipleResponse toPrincipleResponse(LeadershipPrinciple principle) {
        return new LeadershipPrincipleResponse(
                principle.getId(),
                principle.getName(),
                principle.getDescription(),
                principle.getActive()
        );
    }

    private PeerReviewResponse toResponse(PeerReview review) {
        return new PeerReviewResponse(
                review.getId(),
                review.getReviewee().getId(),
                review.getReviewee().getName(),
                review.getPeriodStart(),
                review.getPeriodEnd(),
                review.getPrinciple().getId(),
                review.getPrinciple().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    private PeerReviewPeriodResponse toPeriodResponse(PeerReviewPeriod period) {
        return new PeerReviewPeriodResponse(
                period.getId(),
                period.getName(),
                period.getPeriodStart(),
                period.getPeriodEnd(),
                period.getCreatedAt()
        );
    }

    private PeerReviewAvailableEmployeeResponse toAvailableEmployeeResponse(Employee employee) {
        return new PeerReviewAvailableEmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getRole(),
                employee.getEmploymentType()
        );
    }

    private void ensureInitiatedPeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (peerReviewPeriodRepository.findByPeriodStartAndPeriodEnd(periodStart, periodEnd).isEmpty()) {
            throw new BadRequestException("Peer review period is not initiated");
        }
    }

    private String normalizePeriodName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        return trimmed;
    }

    private void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new BadRequestException("periodStart and periodEnd are required");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new BadRequestException("periodEnd must be on or after periodStart");
        }
    }

    private record PeriodKey(LocalDate periodStart, LocalDate periodEnd) {}

    private List<LeadershipPrinciple> listActivePrincipleEntities() {
        return leadershipPrincipleRepository.findAllByActiveTrueOrderByIdAsc();
    }

    private Map<Long, EmployeeAggregate> buildAggregates(List<PeerReview> reviews) {
        Map<Long, EmployeeAggregate> aggregates = new HashMap<>();
        for (PeerReview review : reviews) {
            // A review record may exist before the reviewer has provided a rating.
            // Unrated reviews should not contribute to points/averages.
            PeerReview.Rating rating = review.getRating();
            if (rating == null) {
                continue;
            }

            Long revieweeId = review.getReviewee().getId();
            EmployeeAggregate aggregate = aggregates.computeIfAbsent(revieweeId, ignored -> new EmployeeAggregate());
            int points = ratingToPoints(rating);
            aggregate.totalPoints += points;
            aggregate.reviewCount += 1;

            PrincipleAggregate principleAggregate = aggregate.principleTotals
                    .computeIfAbsent(review.getPrinciple().getId(), ignored -> new PrincipleAggregate());
            principleAggregate.totalPoints += points;
            principleAggregate.ratingCount += 1;
        }
        return aggregates;
    }

    private List<Employee> resolveEmployeesForAdmin(List<PeerReview> reviews) {
        List<Employee> activeEmployees = employeeRepository.findAllByActiveTrueOrderByNameAsc();
        Map<Long, Employee> byId = new HashMap<>();
        for (Employee employee : activeEmployees) {
            byId.put(employee.getId(), employee);
        }

        Set<Long> revieweeIds = reviews.stream()
                .map(review -> review.getReviewee().getId())
                .collect(Collectors.toSet());
        if (!revieweeIds.isEmpty()) {
            employeeRepository.findAllById(revieweeIds).forEach(employee -> byId.putIfAbsent(employee.getId(), employee));
        }

        return byId.values().stream()
                .sorted(Comparator.comparing(Employee::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private PeerReviewEmployeeResultsResponse toEmployeeResults(Employee employee,
                                                                Map<Long, EmployeeAggregate> aggregates,
                                                                List<LeadershipPrinciple> principles) {
        EmployeeAggregate aggregate = aggregates.get(employee.getId());
        BigDecimal leadershipScore = computeLeadershipScore(aggregate);

        List<PeerReviewPrincipleAverageResponse> averages = principles.stream()
                .map(principle -> toPrincipleAverage(principle, aggregate))
                .toList();

        return new PeerReviewEmployeeResultsResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getRole(),
                employee.getEmploymentType(),
                leadershipScore,
                averages
        );
    }

    private PeerReviewEmployeeSummaryResponse toEmployeeSummary(Employee employee,
                                                               LocalDate periodStart,
                                                               LocalDate periodEnd,
                                                               EmployeeAggregate aggregate) {
        int totalPoints = aggregate == null ? 0 : aggregate.totalPoints;
        int ratingCount = aggregate == null ? 0 : aggregate.reviewCount;
        int maxPoints = ratingCount * 3;
        BigDecimal leadershipScore = computeLeadershipScore(aggregate);

        return new PeerReviewEmployeeSummaryResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getRole(),
                employee.getEmploymentType(),
                periodStart,
                periodEnd,
                totalPoints,
                ratingCount,
                maxPoints,
                leadershipScore
        );
    }

    private PeerReviewPrincipleAverageResponse toPrincipleAverage(LeadershipPrinciple principle,
                                                                  EmployeeAggregate aggregate) {
        if (aggregate == null) {
            return new PeerReviewPrincipleAverageResponse(principle.getId(), principle.getName(), null, 0);
        }
        PrincipleAggregate principleAggregate = aggregate.principleTotals.get(principle.getId());
        if (principleAggregate == null || principleAggregate.ratingCount == 0) {
            return new PeerReviewPrincipleAverageResponse(principle.getId(), principle.getName(), null, 0);
        }
        BigDecimal average = BigDecimal.valueOf(principleAggregate.totalPoints)
                .divide(BigDecimal.valueOf(principleAggregate.ratingCount), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        return new PeerReviewPrincipleAverageResponse(
                principle.getId(),
                principle.getName(),
                average,
                principleAggregate.ratingCount
        );
    }

    private BigDecimal computeLeadershipScore(EmployeeAggregate aggregate) {
        if (aggregate == null || aggregate.reviewCount == 0) {
            return null;
        }
        int maxPoints = aggregate.reviewCount * 3;
        return percentage(aggregate.totalPoints, maxPoints);
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

    private int ratingToPoints(PeerReview.Rating rating) {
        if (rating == null) {
            return 0;
        }
        return switch (rating) {
            case EXCEEDS_THE_BAR -> 3;
            case MEETS_THE_BAR -> 2;
            case NEEDS_IMPROVEMENT -> 1;
        };
    }

    private static class EmployeeAggregate {
        private int totalPoints;
        private int reviewCount;
        private final Map<Long, PrincipleAggregate> principleTotals = new HashMap<>();
    }

    private static class PrincipleAggregate {
        private int totalPoints;
        private long ratingCount;
    }
}
