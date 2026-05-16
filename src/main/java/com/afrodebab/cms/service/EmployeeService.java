package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.*;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.repository.AdminRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmployeeService {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int GENERATED_PASSWORD_LENGTH = 12;

    private final EmployeeRepository employeeRepo;
    private final AdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SendGridEmailService sendGridEmailService;
    private final CloudflareR2Service cloudflareR2Service;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeService(EmployeeRepository employeeRepo,
                           AdminRepository adminRepo,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           SendGridEmailService sendGridEmailService,
                           CloudflareR2Service cloudflareR2Service) {
        this.employeeRepo = employeeRepo;
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sendGridEmailService = sendGridEmailService;
        this.cloudflareR2Service = cloudflareR2Service;
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest req) {
        String normalizedEmail = normalizeEmail(req.email());
        validateGlobalEmailUniqueness(normalizedEmail, null);

        String generatedPassword = generatePassword();
        Employee employee = new Employee();
        employee.setName(req.name());
        employee.setEmail(normalizedEmail);
        employee.setPhone(req.phone());
        employee.setPosition(req.position());
        employee.setLinkedinUrl(req.linkedinUrl());
        employee.setPhoto(req.photo());
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);
        sendGridEmailService.sendEmployeePasswordEmail(employee.getEmail(), employee.getName(), generatedPassword);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createFromForm(String name, String email, String phone, String position,
                                           String linkedinUrl, String photoUrl, MultipartFile photo) {
        String normalizedEmail = normalizeEmail(email);
        validateGlobalEmailUniqueness(normalizedEmail, null);

        String generatedPassword = generatePassword();
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(normalizedEmail);
        employee.setPhone(phone);
        employee.setPosition(position);
        employee.setLinkedinUrl(linkedinUrl);
        employee.setPhoto(photoUrl);
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);

        if (photo != null && !photo.isEmpty()) {
            String photoUrlAfterUpload = cloudflareR2Service.uploadEmployeePhoto(employee.getId(), photo);
            employee.setPhoto(photoUrlAfterUpload);
            employeeRepo.save(employee);
        }

        sendGridEmailService.sendEmployeePasswordEmail(employee.getEmail(), employee.getName(), generatedPassword);
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable) {
        return employeeRepo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest req) {
        Employee employee = getEntityOrThrow(id);

        if (req.email() != null && !req.email().equalsIgnoreCase(employee.getEmail())) {
            String normalizedEmail = normalizeEmail(req.email());
            validateGlobalEmailUniqueness(normalizedEmail, id);
            employee.setEmail(normalizedEmail);
        }

        if (req.name() != null) employee.setName(req.name());
        if (req.phone() != null) employee.setPhone(req.phone());
        if (req.position() != null) employee.setPosition(req.position());
        if (req.linkedinUrl() != null) employee.setLinkedinUrl(req.linkedinUrl());
        if (req.photo() != null) employee.setPhoto(req.photo());
        if (req.active() != null) employee.setActive(req.active());

        employeeRepo.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public void softDelete(Long id) {
        Employee employee = getEntityOrThrow(id);
        employee.setActive(false);
        employeeRepo.save(employee);
    }

    @Transactional
    public EmployeeResponse uploadPhoto(Long id, MultipartFile file) {
        Employee employee = getEntityOrThrow(id);
        String photoUrl = cloudflareR2Service.uploadEmployeePhoto(id, file);
        employee.setPhoto(photoUrl);
        employeeRepo.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public LoginResponse login(EmployeeLoginRequest req) {
        String normalizedEmail = normalizeEmail(req.email());
        Employee employee = employeeRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (!employee.isActive()) throw new BadRequestException("Employee account is inactive");
        if (!passwordEncoder.matches(req.password(), employee.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        employee.setLastLoginAt(Instant.now());
        employeeRepo.save(employee);

        return new LoginResponse(jwtService.generateToken(employee.getEmail(), "EMPLOYEE"));
    }

    @Transactional
    public void changeOwnPassword(String employeeEmail, EmployeeChangePasswordRequest req) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        if (!passwordEncoder.matches(req.currentPassword(), employee.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        employee.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        employeeRepo.save(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getOwnProfile(String employeeEmail) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse uploadOwnPhoto(String employeeEmail, MultipartFile file) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        String photoUrl = cloudflareR2Service.uploadEmployeePhoto(employee.getId(), file);
        employee.setPhoto(photoUrl);
        employeeRepo.save(employee);
        return toResponse(employee);
    }

    private Employee getEntityOrThrow(Long id) {
        return employeeRepo.findById(id).orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void validateGlobalEmailUniqueness(String email, Long currentEmployeeId) {
        if (adminRepo.findByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestException("Email is already used by an admin");
        }

        Optional<Employee> existingEmployee = employeeRepo.findByEmailIgnoreCase(email);
        if (existingEmployee.isPresent() && (currentEmployeeId == null || !existingEmployee.get().getId().equals(currentEmployeeId))) {
            throw new BadRequestException("Employee email already exists");
        }
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getPosition(),
                employee.getLinkedinUrl(),
                employee.getPhoto(),
                employee.isActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
