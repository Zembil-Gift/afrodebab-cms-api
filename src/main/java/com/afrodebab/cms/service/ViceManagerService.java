package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.ViceManagerCreateRequest;
import com.afrodebab.cms.dto.ViceManagerResponse;
import com.afrodebab.cms.dto.ViceManagerUpdateRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailNotification.NotificationType;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.SubOrganization;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.PlatformAdminRepository;
import com.afrodebab.cms.jpa.repository.SubOrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ViceManagerService {

    private static final Logger log = LoggerFactory.getLogger(ViceManagerService.class);
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int GENERATED_PASSWORD_LENGTH = 12;

    private final ManagerRepository managerRepository;
    private final SubOrganizationRepository subOrganizationRepository;
    private final EmployeeRepository employeeRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailTemplateService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ViceManagerService(ManagerRepository managerRepository,
                              SubOrganizationRepository subOrganizationRepository,
                              EmployeeRepository employeeRepository,
                              PlatformAdminRepository platformAdminRepository,
                              PasswordEncoder passwordEncoder,
                              EmailTemplateService emailService) {
        this.managerRepository = managerRepository;
        this.subOrganizationRepository = subOrganizationRepository;
        this.employeeRepository = employeeRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<ViceManagerResponse> listAll() {
        return managerRepository.findAllByRole(Manager.ManagerRole.VICE_MANAGER)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ViceManagerResponse getById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Manager getEntityOrThrow(Long id) {
        return managerRepository.findByIdAndRole(id, Manager.ManagerRole.VICE_MANAGER)
                .orElseThrow(() -> new NotFoundException("Vice Manager not found with id: " + id));
    }

    @Transactional
    public ViceManagerResponse create(ViceManagerCreateRequest req) {
        String normalizedEmail = normalizeEmail(req.email());

        // Validate global email uniqueness across managers, platform admins, and employees
        boolean emailExists = TenantContext.callAsRoot(() ->
                managerRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()
                        || platformAdminRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()
                        || employeeRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()
        );

        if (emailExists) {
            throw new BadRequestException("An account with this email already exists");
        }

        SubOrganization subOrg = subOrganizationRepository.findById(req.subOrganizationId())
                .orElseThrow(() -> new BadRequestException("Sub-organization not found with id: " + req.subOrganizationId()));

        String generatedPassword = generatePassword();
        String name = req.name().trim();

        Manager viceManager = Manager.builder()
                .name(name)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(generatedPassword))
                .role(Manager.ManagerRole.VICE_MANAGER)
                .subOrganization(subOrg)
                .active(true)
                .build();

        Manager saved = managerRepository.save(viceManager);

        try {
            emailService.send(NotificationType.VICE_MANAGER_WELCOME, normalizedEmail, Map.of(
                    "name", name, "email", normalizedEmail, "password", generatedPassword,
                    "branch", subOrg.getName()), TenantContext.get());
        } catch (RuntimeException ex) {
            log.warn("Vice Manager {} created but welcome email failed: {}", normalizedEmail, ex.getMessage());
        }

        return toResponse(saved);
    }

    @Transactional
    public ViceManagerResponse update(Long id, ViceManagerUpdateRequest req) {
        Manager viceManager = getEntityOrThrow(id);

        if (req.name() != null && !req.name().isBlank()) {
            viceManager.setName(req.name().trim());
        }

        if (req.subOrganizationId() != null) {
            SubOrganization subOrg = subOrganizationRepository.findById(req.subOrganizationId())
                    .orElseThrow(() -> new BadRequestException("Sub-organization not found with id: " + req.subOrganizationId()));
            viceManager.setSubOrganization(subOrg);
        }

        if (req.active() != null) {
            viceManager.setActive(req.active());
        }

        return toResponse(managerRepository.save(viceManager));
    }

    @Transactional
    public void delete(Long id) {
        Manager viceManager = getEntityOrThrow(id);
        managerRepository.delete(viceManager);
    }

    private ViceManagerResponse toResponse(Manager manager) {
        Long subOrgId = manager.getSubOrganization() != null ? manager.getSubOrganization().getId() : null;
        String subOrgName = manager.getSubOrganization() != null ? manager.getSubOrganization().getName() : null;

        return new ViceManagerResponse(
                manager.getId(),
                manager.getName(),
                manager.getEmail(),
                manager.isActive(),
                subOrgId,
                subOrgName,
                manager.getLastLoginAt(),
                manager.getCreatedAt(),
                manager.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
