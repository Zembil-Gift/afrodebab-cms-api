package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmailScheduleResponse;
import com.afrodebab.cms.dto.EmailScheduleUpdateRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import com.afrodebab.cms.util.DailySchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Runs each organization's email jobs at the time its manager chose: queued emails are sent
 * daily at the dispatch time, and payroll reminders are queued every N days at that same time
 * (just before the dispatch, so they go out together). The dispatch time is stored in the
 * org's own timezone and converted to UTC on every tick, which keeps DST zones correct.
 */
@Service
public class EmailScheduleService {
    private static final Logger log = LoggerFactory.getLogger(EmailScheduleService.class);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final OrganizationRepository organizationRepo;
    private final EmailNotificationService emailNotificationService;
    private final EmployeePaymentService employeePaymentService;

    public EmailScheduleService(OrganizationRepository organizationRepo,
                                EmailNotificationService emailNotificationService,
                                EmployeePaymentService employeePaymentService) {
        this.organizationRepo = organizationRepo;
        this.emailNotificationService = emailNotificationService;
        this.employeePaymentService = employeePaymentService;
    }

    // Deliberately not @Transactional: each job opens its own session inside TenantContext.callAs,
    // so it is scoped to that org. ponytail: single-instance scheduler; add a lock (e.g. ShedLock) before running replicas.
    @Scheduled(cron = "0 * * * * *", zone = "UTC")
    public void tick() {
        Instant now = Instant.now();
        for (Organization org : organizationRepo.findAll()) {
            if (!"ACTIVE".equals(org.getStatus())) continue;
            try {
                runDueJobs(org, now);
            } catch (RuntimeException ex) {
                log.error("Scheduled email jobs failed for organization {}", org.getId(), ex);
            }
        }
    }

    @Transactional(readOnly = true)
    public EmailScheduleResponse get() {
        return toResponse(getOrgOrThrow(), Instant.now());
    }

    @Transactional
    public EmailScheduleResponse update(EmailScheduleUpdateRequest req) {
        ZoneId zone = parseZone(req.timezone());
        Organization org = getOrgOrThrow();
        org.setEmailDispatchTime(LocalTime.parse(req.dispatchTime(), HH_MM));
        org.setEmailTimezone(zone.getId());
        org.setPayrollReminderIntervalDays(req.payrollReminderIntervalDays());
        return toResponse(organizationRepo.save(org), Instant.now());
    }

    // The last-run marks are written even when a job throws, so a persistent failure is retried at
    // the next scheduled time instead of every minute; failed emails are retried by the next dispatch.
    private void runDueJobs(Organization org, Instant now) {
        TenantContext.callAs(org.getId(), () -> {
            emailNotificationService.dispatchImmediate();
            return null;
        });

        LocalTime time = org.getEmailDispatchTime();
        ZoneId zone = ZoneId.of(org.getEmailTimezone());

        if (DailySchedule.isIntervalDue(org.getLastPayrollReminderAt(), org.getPayrollReminderIntervalDays(), time, zone, now)) {
            try {
                TenantContext.callAs(org.getId(), () -> {
                    employeePaymentService.remindDuePayments();
                    return null;
                });
            } finally {
                organizationRepo.markPayrollReminded(org.getId(), now);
            }
        }

        if (DailySchedule.isDailyDue(org.getLastEmailDispatchAt(), time, zone, now)) {
            try {
                TenantContext.callAs(org.getId(), () -> {
                    emailNotificationService.dispatchPending();
                    return null;
                });
            } finally {
                organizationRepo.markEmailDispatched(org.getId(), now);
            }
        }
    }

    private EmailScheduleResponse toResponse(Organization org, Instant now) {
        LocalTime time = org.getEmailDispatchTime();
        ZoneId zone = ZoneId.of(org.getEmailTimezone());
        LocalTime utcToday = ZonedDateTime.of(LocalDate.ofInstant(now, zone), time, zone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalTime();
        Instant nextDispatch = DailySchedule.isDailyDue(org.getLastEmailDispatchAt(), time, zone, now)
                ? DailySchedule.latestOccurrence(time, zone, now)
                : DailySchedule.nextOccurrence(time, zone, now);
        return new EmailScheduleResponse(
                time.format(HH_MM),
                org.getEmailTimezone(),
                org.getPayrollReminderIntervalDays(),
                utcToday.format(HH_MM),
                nextDispatch,
                DailySchedule.nextIntervalRun(org.getLastPayrollReminderAt(), org.getPayrollReminderIntervalDays(), time, zone, now),
                org.getLastEmailDispatchAt(),
                org.getLastPayrollReminderAt()
        );
    }

    private Organization getOrgOrThrow() {
        return organizationRepo.findById(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private static ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException ex) {
            throw new BadRequestException("Unknown timezone: " + timezone);
        }
    }
}
