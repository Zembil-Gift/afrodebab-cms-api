package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.AdminAttendanceStatusUpdateRequest;
import com.afrodebab.cms.dto.EmployeeConnectedAccountsAdminResponse;
import com.afrodebab.cms.dto.EmployeeCreateRequest;
import com.afrodebab.cms.dto.EmployeeAttendanceResponse;
import com.afrodebab.cms.dto.EmployeeAttendanceUpsertRequest;
import com.afrodebab.cms.dto.EmployeeResponse;
import com.afrodebab.cms.dto.EmployeeUpdateRequest;
import com.afrodebab.cms.service.EmployeeAttendanceService;
import com.afrodebab.cms.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Tag(name = "Admin - Employees")
@RestController
@RequestMapping("/admin/employees")
public class EmployeeAdminController {
    private final EmployeeService service;
    private final EmployeeAttendanceService attendanceService;

    public EmployeeAdminController(EmployeeService service, EmployeeAttendanceService attendanceService) {
        this.service = service;
        this.attendanceService = attendanceService;
    }

    @PostMapping
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest req) {
        return service.create(req);
    }

    @PostMapping(value = "/form", consumes = "multipart/form-data")
    public EmployeeResponse createFromForm(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String position,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String employeeStatus,
            @RequestParam(required = false) String linkedinUrl,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(required = false) String githubUsername,
            @RequestParam(required = false) String trelloUsername,
            @RequestParam(required = false) String telegramUsername,
            @RequestParam(required = false) LocalDate salaryDate,
            @RequestParam(required = false) Long salaryAmountMinor,
            @RequestParam Set<DayOfWeek> salaryScheduleDays,
            @RequestParam(name = "photo", required = false) MultipartFile photo,
            @RequestParam(name = "file", required = false) MultipartFile file
    ) {
        MultipartFile upload = (photo != null && !photo.isEmpty()) ? photo : file;
        return service.createFromForm(
                name,
                email,
                phone,
                position,
                role,
                department,
                employmentType,
                employeeStatus,
                linkedinUrl,
                photoUrl,
                githubUsername,
                trelloUsername,
                telegramUsername,
                salaryDate,
                salaryAmountMinor,
                salaryScheduleDays,
                upload
        );
    }

    @GetMapping
    public Page<EmployeeResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.list(pageable);
    }

    @GetMapping("/with-github")
    public Page<EmployeeResponse> listWithGithubUsername(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.listWithGithubUsername(pageable);
    }

    @GetMapping("/with-trello")
    public Page<EmployeeResponse> listWithTrelloUsername(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.listWithTrelloUsername(pageable);
    }

    @GetMapping("/with-telegram")
    public Page<EmployeeResponse> listWithTelegramUsername(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.listWithTelegramUsername(pageable);
    }

    @GetMapping("/connected-accounts")
    public Page<EmployeeConnectedAccountsAdminResponse> listConnectedAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.listConnectedAccounts(pageable);
    }

    @GetMapping("/by-github/{githubUsername}")
    public EmployeeResponse getByGithubUsername(@PathVariable String githubUsername) {
        return service.getByGithubUsername(githubUsername);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @RequestBody EmployeeUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.softDelete(id);
    }

    @PostMapping("/{id}/photo")
    public EmployeeResponse uploadPhoto(@PathVariable Long id,
                                        @RequestParam(name = "file", required = false) MultipartFile file,
                                        @RequestParam(name = "photo", required = false) MultipartFile photo) {
        MultipartFile upload = (file != null && !file.isEmpty()) ? file : photo;
        return service.uploadPhoto(id, upload);
    }

    @PutMapping("/{id}/attendance")
    public EmployeeAttendanceResponse upsertAttendance(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeAttendanceUpsertRequest req
    ) {
        return attendanceService.upsert(id, req);
    }

    @GetMapping("/{id}/attendance")
    public List<EmployeeAttendanceResponse> getAttendanceHistory(@PathVariable Long id) {
        return attendanceService.history(id);
    }

    @PatchMapping("/{id}/attendance/status")
    public EmployeeAttendanceResponse updateAttendanceFinalStatus(
            @PathVariable Long id,
            @RequestParam LocalDate date,
            @Valid @RequestBody AdminAttendanceStatusUpdateRequest req
    ) {
        return attendanceService.updateFinalStatus(id, date, req);
    }
}
