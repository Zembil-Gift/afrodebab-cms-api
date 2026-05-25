package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.EmployeeAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Long> {
    Optional<EmployeeAttendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
    List<EmployeeAttendance> findAllByEmployeeIdOrderByAttendanceDateDesc(Long employeeId);
    List<EmployeeAttendance> findAllByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long employeeId,
            LocalDate from,
            LocalDate to
    );
}
