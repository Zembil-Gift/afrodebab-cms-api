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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeService {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int GENERATED_PASSWORD_LENGTH = 12;

    private final EmployeeRepository employeeRepo;
    private final AdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailNotificationService emailNotificationService;
    private final CloudflareR2Service cloudflareR2Service;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeService(EmployeeRepository employeeRepo,
                           AdminRepository adminRepo,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           EmailNotificationService emailNotificationService,
                           CloudflareR2Service cloudflareR2Service) {
        this.employeeRepo = employeeRepo;
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailNotificationService = emailNotificationService;
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
        employee.setRole(req.role());
        employee.setDepartment(req.department());
        employee.setEmploymentType(req.employmentType());
        employee.setEmployeeStatus(req.employeeStatus());
        employee.setLinkedinUrl(req.linkedinUrl());
        employee.setPhoto(req.photo());
        employee.setGithubUsername(req.githubUsername());
        employee.setTrelloUsername(req.trelloUsername());
        employee.setTelegramUsername(req.telegramUsername());
        employee.setSalaryEffectiveDate(req.salaryDate());
        employee.setSalaryAmountMinor(req.salaryAmountMinor());
        employee.setOfficeDays(normalizeScheduleDays(req.salaryScheduleDays()));
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);
        emailNotificationService.queueEmployeePasswordEmail(employee.getEmail(), employee.getName(), generatedPassword);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createFromForm(String name, String email, String phone, String position,
                                           String role, String department, String employmentType, String employeeStatus,
                                           String linkedinUrl, String photoUrl, String githubUsername, String trelloUsername, String telegramUsername, LocalDate salaryDate,
                                           Long salaryAmountMinor, Set<DayOfWeek> salaryScheduleDays,
                                           MultipartFile photo) {
        String normalizedEmail = normalizeEmail(email);
        validateGlobalEmailUniqueness(normalizedEmail, null);

        String generatedPassword = generatePassword();
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(normalizedEmail);
        employee.setPhone(phone);
        employee.setPosition(position);
        employee.setRole(role);
        employee.setDepartment(department);
        employee.setEmploymentType(employmentType);
        employee.setEmployeeStatus(employeeStatus);
        employee.setLinkedinUrl(linkedinUrl);
        employee.setPhoto(photoUrl);
        employee.setGithubUsername(githubUsername);
        employee.setTrelloUsername(trelloUsername);
        employee.setTelegramUsername(telegramUsername);
        employee.setSalaryEffectiveDate(salaryDate);
        employee.setSalaryAmountMinor(salaryAmountMinor);
        employee.setOfficeDays(normalizeScheduleDays(salaryScheduleDays));
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);

        if (photo != null && !photo.isEmpty()) {
            String photoUrlAfterUpload = cloudflareR2Service.uploadEmployeePhoto(employee.getId(), photo);
            employee.setPhoto(photoUrlAfterUpload);
            employeeRepo.save(employee);
        }

        emailNotificationService.queueEmployeePasswordEmail(employee.getEmail(), employee.getName(), generatedPassword);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createFromHiredApplication(String name,
                                                       String email,
                                                       String phone,
                                                       String position,
                                                       LocalDate salaryDate,
                                                       Long salaryAmountMinor) {
        return toResponse(createEntityFromHiredApplication(name, email, phone, position, salaryDate, salaryAmountMinor));
    }

    @Transactional
    public Employee createEntityFromHiredApplication(String name,
                                                     String email,
                                                     String phone,
                                                     String position,
                                                     LocalDate salaryDate,
                                                     Long salaryAmountMinor) {
        String normalizedEmail = normalizeEmail(email);
        validateGlobalEmailUniqueness(normalizedEmail, null);

        String generatedPassword = generatePassword();
        Employee employee = new Employee();
        employee.setName(name);
        employee.setEmail(normalizedEmail);
        employee.setPhone(phone);
        employee.setPosition(position);
        employee.setSalaryEffectiveDate(salaryDate);
        employee.setSalaryAmountMinor(salaryAmountMinor);
        employee.setOfficeDays(EnumSet.noneOf(DayOfWeek.class));
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);
        emailNotificationService.queueEmployeePasswordEmail(employee.getEmail(), employee.getName(), generatedPassword);
        return employee;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable) {
        return employeeRepo.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listWithGithubUsername(Pageable pageable) {
        return employeeRepo.findAllWithGithubUsername(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listWithTrelloUsername(Pageable pageable) {
        return employeeRepo.findAllWithTrelloUsername(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listWithTelegramUsername(Pageable pageable) {
        return employeeRepo.findAllWithTelegramUsername(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeConnectedAccountsAdminResponse> listConnectedAccounts(Pageable pageable) {
        return employeeRepo.findAllWithConnectedAccounts(pageable).map(this::toConnectedAccountsAdminResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getByGithubUsername(String githubUsername) {
        if (githubUsername == null || githubUsername.trim().isEmpty()) {
            throw new BadRequestException("githubUsername is required");
        }

        String normalized = githubUsername.trim();
        if (normalized.startsWith("@")) normalized = normalized.substring(1);
        if (normalized.isEmpty()) throw new BadRequestException("githubUsername is required");

        List<Employee> matches = employeeRepo.findAllByGithubUsernameIgnoreCase(normalized);
        if (matches.isEmpty()) throw new NotFoundException("Employee not found");

        if (matches.size() > 1) {
            List<Employee> active = matches.stream().filter(Employee::isActive).toList();
            if (active.size() == 1) return toResponse(active.get(0));
            throw new BadRequestException("Multiple employees found for githubUsername");
        }

        return toResponse(matches.get(0));
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
        if (req.role() != null) employee.setRole(req.role());
        if (req.department() != null) employee.setDepartment(req.department());
        if (req.employmentType() != null) employee.setEmploymentType(req.employmentType());
        if (req.employeeStatus() != null) employee.setEmployeeStatus(req.employeeStatus());
        if (req.linkedinUrl() != null) employee.setLinkedinUrl(req.linkedinUrl());
        if (req.photo() != null) employee.setPhoto(req.photo());
        if (req.githubUsername() != null) employee.setGithubUsername(req.githubUsername());
        if (req.trelloUsername() != null) employee.setTrelloUsername(req.trelloUsername());
        if (req.telegramUsername() != null) employee.setTelegramUsername(req.telegramUsername());
        if (req.active() != null) employee.setActive(req.active());
        if (req.salaryDate() != null) employee.setSalaryEffectiveDate(req.salaryDate());
        if (req.salaryAmountMinor() != null) employee.setSalaryAmountMinor(req.salaryAmountMinor());
        if (req.salaryScheduleDays() != null) employee.setOfficeDays(normalizeScheduleDays(req.salaryScheduleDays()));

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

    @Transactional(readOnly = true)
    public EmployeeConnectedAccountsResponse getOwnConnectedAccounts(String employeeEmail) {
        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        return toConnectedAccountsResponse(employee);
    }

    @Transactional
    public EmployeeConnectedAccountsResponse updateOwnConnectedAccounts(String employeeEmail,
                                                                        EmployeeConnectedAccountsUpdateRequest request) {
        if (request == null || (request.githubUsername() == null
                && request.trelloUsername() == null
                && request.telegramUsername() == null)) {
            throw new BadRequestException("At least one username must be provided");
        }

        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        if (request.githubUsername() != null) {
            employee.setGithubUsername(normalizeUsername(request.githubUsername(), "githubUsername"));
        }
        if (request.trelloUsername() != null) {
            employee.setTrelloUsername(normalizeUsername(request.trelloUsername(), "trelloUsername"));
        }
        if (request.telegramUsername() != null) {
            employee.setTelegramUsername(normalizeUsername(request.telegramUsername(), "telegramUsername"));
        }

        employeeRepo.save(employee);
        return toConnectedAccountsResponse(employee);
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

    @Transactional
    public EmployeeResponse updateOwnProfile(String employeeEmail,
                                             String linkedinUrl,
                                             MultipartFile photo) {
        if ((linkedinUrl == null || linkedinUrl.isBlank()) && (photo == null || photo.isEmpty())) {
            throw new BadRequestException("linkedinUrl or photo must be provided");
        }

        Employee employee = employeeRepo.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        if (linkedinUrl != null) {
            String trimmed = linkedinUrl.trim();
            if (trimmed.isBlank()) {
                throw new BadRequestException("linkedinUrl must not be blank");
            }
            employee.setLinkedinUrl(trimmed);
        }

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = cloudflareR2Service.uploadEmployeePhoto(employee.getId(), photo);
            employee.setPhoto(photoUrl);
        }

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
        List<DayOfWeek> officeDays = new ArrayList<>(employee.getOfficeDays());
        officeDays.sort(DayOfWeek::compareTo);
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getPosition(),
                employee.getRole(),
                employee.getDepartment(),
                employee.getEmploymentType(),
                employee.getEmployeeStatus(),
                employee.getLinkedinUrl(),
                employee.getPhoto(),
                employee.getGithubUsername(),
                employee.getTrelloUsername(),
                employee.getTelegramUsername(),
                employee.isActive(),
                employee.getSalaryEffectiveDate(),
                employee.getSalaryAmountMinor(),
                officeDays,
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    private EmployeeConnectedAccountsResponse toConnectedAccountsResponse(Employee employee) {
        return new EmployeeConnectedAccountsResponse(
                employee.getId(),
                employee.getName(),
                employee.getGithubUsername(),
                employee.getTrelloUsername(),
                employee.getTelegramUsername()
        );
    }

    private EmployeeConnectedAccountsAdminResponse toConnectedAccountsAdminResponse(Employee employee) {
        return new EmployeeConnectedAccountsAdminResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getGithubUsername(),
                employee.getTrelloUsername(),
                employee.getTelegramUsername()
        );
    }

    private Set<DayOfWeek> normalizeScheduleDays(Set<DayOfWeek> salaryScheduleDays) {
        if (salaryScheduleDays == null || salaryScheduleDays.isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return EnumSet.copyOf(new HashSet<>(salaryScheduleDays));
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

    private String normalizeUsername(String username, String fieldName) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
