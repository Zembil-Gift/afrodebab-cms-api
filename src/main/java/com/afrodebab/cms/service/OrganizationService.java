package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.OrgCreateRequest;
import com.afrodebab.cms.dto.OrgProfileUpdateRequest;
import com.afrodebab.cms.dto.OrgResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailNotification.NotificationType;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.entity.SignupRequest;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
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

/** Platform-admin organization lifecycle: create org + first manager, list, suspend/activate. */
@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final int GENERATED_PASSWORD_LENGTH = 12;

    private final OrganizationRepository orgRepo;
    private final ManagerRepository managerRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailTemplateService emailService;
    private final SignupService signupService;
    private final SubOrganizationService subOrganizationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrganizationService(OrganizationRepository orgRepo, ManagerRepository managerRepo,
                               PasswordEncoder passwordEncoder, EmailTemplateService emailService,
                               SignupService signupService, SubOrganizationService subOrganizationService) {
        this.orgRepo = orgRepo;
        this.managerRepo = managerRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.signupService = signupService;
        this.subOrganizationService = subOrganizationService;
    }

    /**
     * Not {@code @Transactional}: the org row and its first manager are saved in separate
     * sessions so the manager insert can open under the new org's tenant scope (Hibernate
     * fixes the tenant at session open). The org slug and manager email unique constraints
     * guard integrity.
     */
    public OrgResponse create(OrgCreateRequest req) {
        String slug = req.slug().trim().toLowerCase(Locale.ROOT);
        if (orgRepo.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("An organization with this slug already exists");
        }
        String managerEmail = req.managerEmail().trim().toLowerCase(Locale.ROOT);
        // Manager email is globally unique; check across all orgs.
        boolean emailTaken = TenantContext.callAsRoot(
                () -> managerRepo.findByEmailIgnoreCase(managerEmail).isPresent());
        if (emailTaken) {
            throw new BadRequestException("A manager with this email already exists");
        }

        // Seed the profile from the create request, backfilling from the linked signup request.
        SignupRequest lead = signupService.find(req.requestId()).orElse(null);
        Organization org = orgRepo.save(Organization.builder()
                .name(req.name().trim())
                .slug(slug)
                .status("ACTIVE")
                .plan("FREE")
                .phone(firstNonBlank(req.phone(), lead != null ? lead.getPhone() : null))
                .country(trimToNull(req.country()))
                .businessType(trimToNull(req.businessType()))
                .industry(lead != null ? lead.getIndustry() : null)
                .websiteUrl(firstNonBlank(req.websiteUrl(), lead != null ? lead.getWebsiteUrl() : null))
                .companyEmail(firstNonBlank(req.companyEmail(), lead != null ? lead.getEmail() : null))
                .build());

        // Password is generated server-side and emailed to the manager (never chosen by the admin).
        String generatedPassword = generatePassword();
        String managerName = req.managerName().trim();

        // Create the first manager under the new org's scope so @TenantId is populated.
        TenantContext.callAs(org.getId(), () -> managerRepo.save(Manager.builder()
                .name(managerName)
                .email(managerEmail)
                .passwordHash(passwordEncoder.encode(generatedPassword))
                .active(true)
                .build()));

        // Create the default sub-organization under the new org's scope.
        TenantContext.callAs(org.getId(), () -> subOrganizationService.createDefaultSubOrganization(org.getName()));

        // Best-effort: a failed credentials email must not roll back a created organization.
        try {
            emailService.send(NotificationType.MANAGER_WELCOME, managerEmail, Map.of(
                    "name", managerName, "email", managerEmail, "password", generatedPassword), org.getId());
        } catch (RuntimeException ex) {
            log.warn("Organization {} created but manager welcome email to {} failed: {}",
                    org.getSlug(), managerEmail, ex.getMessage());
        }

        // If this org was provisioned from a self-serve signup request, close that request out.
        if (req.requestId() != null) {
            signupService.markApproved(req.requestId());
        }

        return OrgResponse.from(org);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    // --- Manager-facing company profile (operates on the caller's own org via TenantContext) ---

    /** The organization the current manager belongs to (from the tenant already in scope). */
    @Transactional(readOnly = true)
    public OrgResponse getMyOrg() {
        return OrgResponse.from(currentOrg());
    }

    @Transactional
    public OrgResponse updateMyOrg(OrgProfileUpdateRequest req) {
        Organization org = currentOrg();
        org.setName(req.name().trim());
        org.setTagline(trimToNull(req.tagline()));
        org.setDescription(trimToNull(req.description()));
        org.setLogoUrl(trimToNull(req.logoUrl()));
        org.setCoverImageUrl(trimToNull(req.coverImageUrl()));
        org.setBusinessType(trimToNull(req.businessType()));
        org.setIndustry(trimToNull(req.industry()));
        org.setCompanySize(trimToNull(req.companySize()));
        org.setFoundedYear(req.foundedYear());
        org.setPhone(trimToNull(req.phone()));
        org.setCompanyEmail(trimToNull(req.companyEmail()));
        org.setWebsiteUrl(trimToNull(req.websiteUrl()));
        org.setAddressLine(trimToNull(req.addressLine()));
        org.setCity(trimToNull(req.city()));
        org.setCountry(trimToNull(req.country()));
        org.setLinkedinUrl(trimToNull(req.linkedinUrl()));
        org.setTwitterUrl(trimToNull(req.twitterUrl()));
        org.setFacebookUrl(trimToNull(req.facebookUrl()));
        org.setInstagramUrl(trimToNull(req.instagramUrl()));
        return OrgResponse.from(orgRepo.save(org));
    }

    /** Persist a freshly uploaded logo URL on the current manager's org. */
    @Transactional
    public OrgResponse setLogoUrl(String url) {
        Organization org = currentOrg();
        org.setLogoUrl(url);
        return OrgResponse.from(orgRepo.save(org));
    }

    /** Persist a freshly uploaded cover image URL on the current manager's org. */
    @Transactional
    public OrgResponse setCoverImageUrl(String url) {
        Organization org = currentOrg();
        org.setCoverImageUrl(url);
        return OrgResponse.from(orgRepo.save(org));
    }

    private Organization currentOrg() {
        Long orgId = TenantContext.get();
        if (orgId == null) throw new NotFoundException("Organization not found");
        return orgRepo.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private static String trimToNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> list() {
        return orgRepo.findAll().stream().map(OrgResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrgResponse get(Long id) {
        return OrgResponse.from(orgRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found")));
    }

    @Transactional
    public OrgResponse setStatus(Long id, String status) {
        Organization org = orgRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        org.setStatus(status);
        return OrgResponse.from(orgRepo.save(org));
    }
}
