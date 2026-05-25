package com.afrodebab.cms.service;

import com.afrodebab.cms.config.AttendancePolicyProperties;
import com.afrodebab.cms.dto.AdminAttendanceStatusUpdateRequest;
import com.afrodebab.cms.dto.EmployeeAttendanceResponse;
import com.afrodebab.cms.dto.EmployeeAttendanceUpsertRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.EmployeeAttendance;
import com.afrodebab.cms.jpa.repository.EmployeeAttendanceRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeAttendanceService {
    private final EmployeeAttendanceRepository employeeAttendanceRepo;
    private final EmployeeRepository employeeRepo;
    private final AttendancePolicyProperties attendancePolicyProperties;

    public EmployeeAttendanceService(EmployeeAttendanceRepository employeeAttendanceRepo,
                                     EmployeeRepository employeeRepo,
                                     AttendancePolicyProperties attendancePolicyProperties) {
        this.employeeAttendanceRepo = employeeAttendanceRepo;
        this.employeeRepo = employeeRepo;
        this.attendancePolicyProperties = attendancePolicyProperties;
    }

    @Transactional
    public EmployeeAttendanceResponse upsert(Long employeeId, EmployeeAttendanceUpsertRequest req) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        validateChronology(req.clockInAt(), req.clockOutAt(), req.lunchBreakInAt(), req.lunchBreakOutAt());
        validateTimesMatchDate(req.date(), req.clockInAt(), req.clockOutAt(), req.lunchBreakInAt(), req.lunchBreakOutAt());

        EmployeeAttendance attendance = employeeAttendanceRepo
                .findByEmployeeIdAndAttendanceDate(employeeId, req.date())
                .orElse(new EmployeeAttendance());

        attendance.setEmployee(employee);
        attendance.setAttendanceDate(req.date());
        attendance.setClockInAt(req.clockInAt());
        attendance.setClockOutAt(req.clockOutAt());
        attendance.setLunchBreakInAt(req.lunchBreakInAt());
        attendance.setLunchBreakOutAt(req.lunchBreakOutAt());
        attendance.setAttendanceStatus(computeAttendanceStatus(
                req.clockInAt(),
                req.clockOutAt(),
                req.lunchBreakInAt(),
                req.lunchBreakOutAt(),
                null
        ));
        attendance.setNotes(req.notes());

        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse updateFinalStatus(Long employeeId,
                                                        LocalDate date,
                                                        AdminAttendanceStatusUpdateRequest req) {
        if (!employeeRepo.existsById(employeeId)) {
            throw new NotFoundException("Employee not found");
        }
        EmployeeAttendance attendance = employeeAttendanceRepo.findByEmployeeIdAndAttendanceDate(employeeId, date)
                .orElseThrow(() -> new NotFoundException("Attendance record not found for the provided date"));

        Map<String, String> status = attendance.getAttendanceStatus() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(attendance.getAttendanceStatus());
        status.put("final", req.finalStatus().name());
        attendance.setAttendanceStatus(status);
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<EmployeeAttendanceResponse> history(Long employeeId) {
        if (!employeeRepo.existsById(employeeId)) {
            throw new NotFoundException("Employee not found");
        }

        return employeeAttendanceRepo.findAllByEmployeeIdOrderByAttendanceDateDesc(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EmployeeAttendanceResponse toResponse(EmployeeAttendance attendance) {
        return new EmployeeAttendanceResponse(
                attendance.getId(),
                attendance.getEmployee().getId(),
                attendance.getAttendanceDate(),
                attendance.getClockInAt(),
                attendance.getClockOutAt(),
                attendance.getLunchBreakInAt(),
                attendance.getLunchBreakOutAt(),
                attendance.getAttendanceStatus(),
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }

    @Transactional
    public EmployeeAttendanceResponse clockIn(String email) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        validateAttendanceAllowedForDate(employee, today);
        Optional<EmployeeAttendance> existing = employeeAttendanceRepo.findByEmployeeIdAndAttendanceDate(employee.getId(), today);

        if (existing.isPresent()) {
            throw new BadRequestException("Already clocked in today");
        }

        EmployeeAttendance attendance = new EmployeeAttendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(today);
        attendance.setClockInAt(Instant.now());
        attendance.setAttendanceStatus(computeAttendanceStatus(
                attendance.getClockInAt(),
                attendance.getClockOutAt(),
                attendance.getLunchBreakInAt(),
                attendance.getLunchBreakOutAt(),
                null
        ));

        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse clockOut(String email) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        validateAttendanceAllowedForDate(employee, today);
        EmployeeAttendance attendance = employeeAttendanceRepo.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new NotFoundException("No clock-in record found for today"));

        if (attendance.getClockOutAt() != null) {
            throw new BadRequestException("Already clocked out today");
        }

        if (attendance.getLunchBreakInAt() != null && attendance.getLunchBreakOutAt() == null) {
            throw new BadRequestException("Cannot clock out while lunch break is active");
        }

        Instant now = Instant.now();
        if (now.isBefore(attendance.getClockInAt())) {
            throw new BadRequestException("clockOutAt must be later than or equal to clockInAt");
        }
        if (attendance.getLunchBreakOutAt() != null && now.isBefore(attendance.getLunchBreakOutAt())) {
            throw new BadRequestException("clockOutAt must be later than or equal to lunchBreakOutAt");
        }

        attendance.setClockOutAt(now);
        attendance.setAttendanceStatus(computeAttendanceStatus(
                attendance.getClockInAt(),
                attendance.getClockOutAt(),
                attendance.getLunchBreakInAt(),
                attendance.getLunchBreakOutAt(),
                null
        ));
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse lunchBreakIn(String email) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        validateAttendanceAllowedForDate(employee, today);
        EmployeeAttendance attendance = employeeAttendanceRepo.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new NotFoundException("No clock-in record found for today"));

        if (attendance.getClockOutAt() != null) {
            throw new BadRequestException("Already clocked out today");
        }
        if (attendance.getLunchBreakInAt() != null) {
            throw new BadRequestException("Lunch break already started");
        }

        Instant now = Instant.now();
        if (now.isBefore(attendance.getClockInAt())) {
            throw new BadRequestException("lunchBreakInAt must be later than or equal to clockInAt");
        }

        attendance.setLunchBreakInAt(now);
        attendance.setAttendanceStatus(computeAttendanceStatus(
                attendance.getClockInAt(),
                attendance.getClockOutAt(),
                attendance.getLunchBreakInAt(),
                attendance.getLunchBreakOutAt(),
                null
        ));
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse lunchBreakOut(String email) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        validateAttendanceAllowedForDate(employee, today);
        EmployeeAttendance attendance = employeeAttendanceRepo.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new NotFoundException("No clock-in record found for today"));

        if (attendance.getClockOutAt() != null) {
            throw new BadRequestException("Already clocked out today");
        }
        if (attendance.getLunchBreakInAt() == null) {
            throw new BadRequestException("Lunch break has not started");
        }
        if (attendance.getLunchBreakOutAt() != null) {
            throw new BadRequestException("Lunch break already ended");
        }

        Instant now = Instant.now();
        if (!now.isAfter(attendance.getLunchBreakInAt())) {
            throw new BadRequestException("lunchBreakOutAt must be later than lunchBreakInAt");
        }

        attendance.setLunchBreakOutAt(now);
        attendance.setAttendanceStatus(computeAttendanceStatus(
                attendance.getClockInAt(),
                attendance.getClockOutAt(),
                attendance.getLunchBreakInAt(),
                attendance.getLunchBreakOutAt(),
                null
        ));
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    private void validateChronology(Instant clockInAt,
                                    Instant clockOutAt,
                                    Instant lunchBreakInAt,
                                    Instant lunchBreakOutAt) {
        boolean hasClockIn = clockInAt != null;
        boolean hasClockOut = clockOutAt != null;
        if (hasClockIn != hasClockOut) {
            throw new BadRequestException("clockInAt and clockOutAt must both be provided or both be null");
        }

        if (hasClockIn && !clockOutAt.isAfter(clockInAt)) {
            throw new BadRequestException("clockOutAt must be later than clockInAt");
        }

        boolean hasLunchBreakIn = lunchBreakInAt != null;
        boolean hasLunchBreakOut = lunchBreakOutAt != null;
        if (hasLunchBreakIn != hasLunchBreakOut) {
            throw new BadRequestException("lunchBreakInAt and lunchBreakOutAt must both be provided");
        }

        if (!hasLunchBreakIn) {
            return;
        }

        if (!hasClockIn) {
            throw new BadRequestException("lunch break requires clockInAt and clockOutAt");
        }

        if (lunchBreakInAt.isBefore(clockInAt)) {
            throw new BadRequestException("lunchBreakInAt must be later than or equal to clockInAt");
        }
        if (!lunchBreakOutAt.isAfter(lunchBreakInAt)) {
            throw new BadRequestException("lunchBreakOutAt must be later than lunchBreakInAt");
        }
        if (lunchBreakOutAt.isAfter(clockOutAt)) {
            throw new BadRequestException("lunchBreakOutAt must be earlier than or equal to clockOutAt");
        }
    }

    private void validateTimesMatchDate(LocalDate date,
                                        Instant clockInAt,
                                        Instant clockOutAt,
                                        Instant lunchBreakInAt,
                                        Instant lunchBreakOutAt) {
        if (clockInAt != null && !clockInAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("clockInAt must match the provided date");
        }
        if (clockOutAt != null && !clockOutAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("clockOutAt must match the provided date");
        }

        if (lunchBreakInAt != null && !lunchBreakInAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("lunchBreakInAt must match the provided date");
        }
        if (lunchBreakOutAt != null && !lunchBreakOutAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("lunchBreakOutAt must match the provided date");
        }
    }

    private Map<String, String> computeAttendanceStatus(Instant clockInAt,
                                                        Instant clockOutAt,
                                                        Instant lunchBreakInAt,
                                                        Instant lunchBreakOutAt,
                                                        EmployeeAttendance.AttendanceFinalStatus finalStatusOverride) {
        LocalTime entryBaseline = attendancePolicyProperties.getEntryTime();
        LocalTime exitBaseline = attendancePolicyProperties.getExitTime();
        LocalTime lunchStartBaseline = attendancePolicyProperties.getLunchStartTime();
        LocalTime lunchEndBaseline = attendancePolicyProperties.getLunchEndTime();
        int graceMinutes = attendancePolicyProperties.getGraceMinutes();
        int maxLunchBreakMinutes = attendancePolicyProperties.getMaxLunchBreakMinutes();

        String entryStatus = "ABSENT";
        String exitStatus = "ABSENT";
        String lunchStatus = "ABSENT";
        boolean isLate = false;

        if (clockInAt != null) {
            LocalTime entryTime = clockInAt.atOffset(ZoneOffset.UTC).toLocalTime();
            if (entryTime.isAfter(entryBaseline.plusMinutes(graceMinutes))) {
                entryStatus = "LATE_IN";
                isLate = true;
            } else {
                entryStatus = "ON_TIME";
            }
        }

        if (clockOutAt != null) {
            LocalTime exitTime = clockOutAt.atOffset(ZoneOffset.UTC).toLocalTime();
            if (exitTime.isBefore(exitBaseline.minusMinutes(graceMinutes))) {
                exitStatus = "EARLY_OUT";
                isLate = true;
            } else if (exitTime.isAfter(exitBaseline.plusMinutes(graceMinutes))) {
                exitStatus = "LATE_OUT";
                isLate = true;
            } else {
                exitStatus = "ON_TIME";
            }
        }

        boolean lunchExceeded = false;
        boolean lunchProvided = lunchBreakInAt != null && lunchBreakOutAt != null;
        if (lunchProvided) {
            LocalTime lunchInTime = lunchBreakInAt.atOffset(ZoneOffset.UTC).toLocalTime();
            LocalTime lunchOutTime = lunchBreakOutAt.atOffset(ZoneOffset.UTC).toLocalTime();

            if (lunchInTime.isAfter(lunchStartBaseline.plusMinutes(graceMinutes))
                    || lunchOutTime.isAfter(lunchEndBaseline.plusMinutes(graceMinutes))) {
                lunchStatus = "LATE_OUT";
                isLate = true;
            } else {
                lunchStatus = "ON_TIME";
            }

            long lunchDuration = Duration.between(lunchBreakInAt, lunchBreakOutAt).toMinutes();
            if (lunchDuration > maxLunchBreakMinutes) {
                lunchStatus = "ABSENT";
                lunchExceeded = true;
            }
        }

        EmployeeAttendance.AttendanceFinalStatus finalStatus;
        if (finalStatusOverride != null) {
            finalStatus = finalStatusOverride;
        } else if (clockInAt == null) {
            finalStatus = EmployeeAttendance.AttendanceFinalStatus.ABSENT;
        } else if (clockOutAt == null) {
            finalStatus = "LATE_IN".equals(entryStatus)
                    ? EmployeeAttendance.AttendanceFinalStatus.LATE
                    : EmployeeAttendance.AttendanceFinalStatus.ON_TIME;
        } else if (!lunchProvided || lunchExceeded) {
            finalStatus = EmployeeAttendance.AttendanceFinalStatus.ABSENT;
        } else if (isLate) {
            finalStatus = EmployeeAttendance.AttendanceFinalStatus.LATE;
        } else {
            finalStatus = EmployeeAttendance.AttendanceFinalStatus.ON_TIME;
        }

        Map<String, String> status = new LinkedHashMap<>();
        status.put("entry", entryStatus);
        status.put("exit", exitStatus);
        status.put("lunch", lunchStatus);
        status.put("final", finalStatus.name());
        return status;
    }

    private void validateAttendanceAllowedForDate(Employee employee, LocalDate date) {
        Set<DayOfWeek> officeScheduleDays = employee.getOfficeDays();
        if (officeScheduleDays == null || officeScheduleDays.isEmpty()) {
            throw new BadRequestException("No office schedule days are configured for this employee");
        }

        if (officeScheduleDays.contains(date.getDayOfWeek())) {
            return;
        }

        List<DayOfWeek> sortedDays = new ArrayList<>(officeScheduleDays);
        sortedDays.sort(DayOfWeek::compareTo);
        throw new BadRequestException("Attendance is only allowed on office schedule days: " + sortedDays);
    }
}
