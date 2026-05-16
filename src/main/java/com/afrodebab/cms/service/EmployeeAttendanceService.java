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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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

        if (!req.clockOutAt().isAfter(req.clockInAt())) {
            throw new BadRequestException("clockOutAt must be later than clockInAt");
        }

        if (!req.clockInAt().atOffset(ZoneOffset.UTC).toLocalDate().equals(req.date())
                || !req.clockOutAt().atOffset(ZoneOffset.UTC).toLocalDate().equals(req.date())) {
            throw new BadRequestException("clockInAt and clockOutAt must match the provided date");
        }

        EmployeeAttendance attendance = employeeAttendanceRepo
                .findByEmployeeIdAndAttendanceDate(employeeId, req.date())
                .orElse(new EmployeeAttendance());

        attendance.setEmployee(employee);
        attendance.setAttendanceDate(req.date());
        attendance.setClockInAt(req.clockInAt());
        attendance.setClockOutAt(req.clockOutAt());

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
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }

    @Transactional
    public EmployeeAttendanceResponse clockIn(String email) {
        Employee employee = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
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
        EmployeeAttendance attendance = employeeAttendanceRepo.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new NotFoundException("No clock-in record found for today"));

        if (attendance.getClockOutAt() != null) {
            throw new BadRequestException("Already clocked out today");
        }

        attendance.setClockOutAt(Instant.now());
        employeeAttendanceRepo.save(attendance);
        return toResponse(attendance);
    }
}
