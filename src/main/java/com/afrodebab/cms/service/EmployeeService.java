package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.*;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
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

import com.afrodebab.cms.jpa.entity.SubOrganization;
import com.afrodebab.cms.jpa.repository.SubOrganizationRepository;

@Service
public class EmployeeService {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int GENERATED_PASSWORD_LENGTH = 12;

    private final EmployeeRepository employeeRepo;
    private final ManagerRepository adminRepo;
    private final SubOrganizationRepository subOrganizationRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailNotificationService emailNotificationService;
    private final CloudflareR2Service cloudflareR2Service;
    private final EmailUniquenessService emailUniquenessService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeService(EmployeeRepository employeeRepo,
                           ManagerRepository adminRepo,
                           SubOrganizationRepository subOrganizationRepo,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           EmailNotificationService emailNotificationService,
                           CloudflareR2Service cloudflareR2Service,
                           EmailUniquenessService emailUniquenessService) {
        this.employeeRepo = employeeRepo;
        this.adminRepo = adminRepo;
        this.subOrganizationRepo = subOrganizationRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailNotificationService = emailNotificationService;
        this.cloudflareR2Service = cloudflareR2Service;
        this.emailUniquenessService = emailUniquenessService;
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
        employee.setSubOrganization(resolveSubOrganization(req.subOrganizationId()));
        employee.setSalaryEffectiveDate(req.salaryDate());
        employee.setSalaryAmountMinor(req.salaryAmountMinor());
        employee.setOfficeDays(normalizeScheduleDays(req.salaryScheduleDays()));
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);
        queueNewEmployeeEmails(employee, generatedPassword);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createFromForm(String name, String email, String phone, String position,
                                           String role, String department, String employmentType, String employeeStatus,
                                           String linkedinUrl, String photoUrl, String githubUsername, String trelloUsername, String telegramUsername,
                                           Long subOrganizationId, LocalDate salaryDate,
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
        employee.setSubOrganization(resolveSubOrganization(subOrganizationId));
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

        queueNewEmployeeEmails(employee, generatedPassword);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse createFromHiredApplication(String name,
                                                       String email,
                                                       String phone,
                                                       String position,
                                                       Long subOrganizationId,
                                                       LocalDate salaryDate,
                                                       Long salaryAmountMinor) {
        return toResponse(createEntityFromHiredApplication(name, email, phone, position, subOrganizationId, salaryDate, salaryAmountMinor));
    }

    @Transactional
    public Employee createEntityFromHiredApplication(String name,
                                                     String email,
                                                     String phone,
                                                     String position,
                                                     Long subOrganizationId,
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
        employee.setSubOrganization(resolveSubOrganization(subOrganizationId));
        employee.setSalaryEffectiveDate(salaryDate);
        employee.setSalaryAmountMinor(salaryAmountMinor);
        employee.setOfficeDays(EnumSet.noneOf(DayOfWeek.class));
        employee.setPasswordHash(passwordEncoder.encode(generatedPassword));
        employee.setActive(true);

        employeeRepo.save(employee);
        queueNewEmployeeEmails(employee, generatedPassword);
        return employee;
    }

    @Transactional
    public Employee createEntityFromHiredApplication(String name,
                                                     String email,
                                                     String phone,
                                                     String position,
                                                     LocalDate salaryDate,
                                                     Long salaryAmountMinor) {
        return createEntityFromHiredApplication(name, email, phone, position, null, salaryDate, salaryAmountMinor);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable) {
        return list(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Pageable pageable, Long subOrganizationId) {
        if (subOrganizationId != null) {
            return employeeRepo.findAllBySubOrganizationId(subOrganizationId, pageable).map(this::toResponse);
        }
        return employeeRepo.findAll(pageable).map(this::toResponse);
    }

    /** Credentials for the new employee, plus a heads-up to the vice managers of their branch. */
    private void queueNewEmployeeEmails(Employee employee, String generatedPassword) {
        emailNotificationService.queueEmployeePasswordEmail(employee.getEmail(), employee.getName(), generatedPassword);
        SubOrganization branch = employee.getSubOrganization();
        if (branch == null) return;
        adminRepo.findAllByActiveTrueAndRoleAndSubOrganizationId(Manager.ManagerRole.VICE_MANAGER, branch.getId())
                .forEach(vice -> emailNotificationService.queueViceManagerNewEmployeeEmail(
                        vice.getEmail(), vice.getName(), branch.getName(),
                        employee.getName(), employee.getEmail(), employee.getPosition()));
    }

    private SubOrganization resolveSubOrganization(Long subOrganizationId) {
        if (subOrganizationId != null) {
            return subOrganizationRepo.findById(subOrganizationId)
                    .orElseThrow(() -> new BadRequestException("Sub-organization not found with id: " + subOrganizationId));
        }
        return subOrganizationRepo.findByIsDefaultTrue()
                .orElseThrow(() -> new BadRequestException("Default sub-organization not found"));
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
        return listWithTelegramUsername(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> listWithTelegramUsername(Pageable pageable, Long subOrganizationId) {
        return employeeRepo.findAllWithTelegramUsername(subOrganizationId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeConnectedAccountsAdminResponse> listConnectedAccounts(Pageable pageable) {
        return listConnectedAccounts(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeConnectedAccountsAdminResponse> listConnectedAccounts(Pageable pageable, Long subOrganizationId) {
        return employeeRepo.findAllWithConnectedAccounts(subOrganizationId, pageable).map(this::toConnectedAccountsAdminResponse);
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

        // A new sign-in email gets a fresh password sent there: the old inbox may no longer be the employee's.
        String newPassword = null;
        if (req.email() != null && !normalizeEmail(req.email()).equals(employee.getEmail())) {
            String normalizedEmail = normalizeEmail(req.email());
            validateGlobalEmailUniqueness(normalizedEmail, id);
            employee.setEmail(normalizedEmail);
            newPassword = generatePassword();
            employee.setPasswordHash(passwordEncoder.encode(newPassword));
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
        if (req.subOrganizationId() != null) {
            SubOrganization subOrg = subOrganizationRepo.findById(req.subOrganizationId())
                    .orElseThrow(() -> new BadRequestException("Sub-organization not found with id: " + req.subOrganizationId()));
            employee.setSubOrganization(subOrg);
        }
        if (req.salaryDate() != null) employee.setSalaryEffectiveDate(req.salaryDate());
        if (req.salaryAmountMinor() != null) employee.setSalaryAmountMinor(req.salaryAmountMinor());
        if (req.salaryScheduleDays() != null) employee.setOfficeDays(normalizeScheduleDays(req.salaryScheduleDays()));

        employeeRepo.save(employee);
        if (newPassword != null) {
            emailNotificationService.queueEmployeeEmailChangedEmail(employee.getEmail(), employee.getName(), newPassword);
        }
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

    /**
     * Must be invoked in root tenant scope (see EmployeeAuthController) so the employee can
     * be resolved by their globally-unique email before any org is known.
     */
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

        return new LoginResponse(
                jwtService.generateToken(employee.getEmail(), "EMPLOYEE", employee.getOrganizationId()));
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
        emailUniquenessService.assertAccountEmailAvailable(email, EmailUniquenessService.AccountType.EMPLOYEE, currentEmployeeId);
    }

    private EmployeeResponse toResponse(Employee employee) {
        List<DayOfWeek> officeDays = new ArrayList<>(employee.getOfficeDays());
        officeDays.sort(DayOfWeek::compareTo);
        Long subOrgId = employee.getSubOrganization() != null ? employee.getSubOrganization().getId() : null;
        String subOrgName = employee.getSubOrganization() != null ? employee.getSubOrganization().getName() : null;

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
                subOrgId,
                subOrgName,
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
