package com.afrodebab.cms.service;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeAttendanceService {
    private final EmployeeAttendanceRepository employeeAttendanceRepo;
    private final EmployeeRepository employeeRepo;

    public EmployeeAttendanceService(EmployeeAttendanceRepository employeeAttendanceRepo, EmployeeRepository employeeRepo) {
        this.employeeAttendanceRepo = employeeAttendanceRepo;
        this.employeeRepo = employeeRepo;
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
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }

    @Transactional
    public EmployeeAttendanceResponse clockIn(String email) {
        Employee employee = employeeRepo.findByEmail(email)
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

        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse clockOut(String email) {
        Employee employee = employeeRepo.findByEmail(email)
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
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse lunchBreakIn(String email) {
        Employee employee = employeeRepo.findByEmail(email)
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
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public EmployeeAttendanceResponse lunchBreakOut(String email) {
        Employee employee = employeeRepo.findByEmail(email)
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
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }

    private void validateChronology(Instant clockInAt,
                                    Instant clockOutAt,
                                    Instant lunchBreakInAt,
                                    Instant lunchBreakOutAt) {
        if (!clockOutAt.isAfter(clockInAt)) {
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
        if (!clockInAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)
                || !clockOutAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("clockInAt and clockOutAt must match the provided date");
        }

        if (lunchBreakInAt != null && !lunchBreakInAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("lunchBreakInAt must match the provided date");
        }
        if (lunchBreakOutAt != null && !lunchBreakOutAt.atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
            throw new BadRequestException("lunchBreakOutAt must match the provided date");
        }
    }

    private void validateAttendanceAllowedForDate(Employee employee, LocalDate date) {
        Set<DayOfWeek> officeScheduleDays = employee.getSalaryScheduleDays();
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
