package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.SignupOtpRequest;
import com.afrodebab.cms.dto.SignupRequestResponse;
import com.afrodebab.cms.dto.SignupSubmitRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailNotification.NotificationType;
import com.afrodebab.cms.jpa.entity.PlatformAdmin;
import com.afrodebab.cms.jpa.entity.EmailOtp;
import com.afrodebab.cms.jpa.entity.SignupRequest;
import com.afrodebab.cms.jpa.repository.PlatformAdminRepository;
import com.afrodebab.cms.jpa.repository.SignupRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Handles public "Start free" submissions and their platform-admin review lifecycle. */
@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);

    private final SignupRequestRepository signupRepo;
    private final EmailOtpService otpService;
    private final PlatformAdminRepository platformAdminRepo;
    private final EmailTemplateService emailService;
    private final EmailUniquenessService emailUniquenessService;

    public SignupService(SignupRequestRepository signupRepo,
                         EmailOtpService otpService,
                         PlatformAdminRepository platformAdminRepo,
                         EmailTemplateService emailService,
                         EmailUniquenessService emailUniquenessService) {
        this.signupRepo = signupRepo;
        this.otpService = otpService;
        this.platformAdminRepo = platformAdminRepo;
        this.emailService = emailService;
        this.emailUniquenessService = emailUniquenessService;
    }

    /** Emails a fresh 6-digit code, replacing any earlier one for this address. */
    @Transactional
    public void sendOtp(SignupOtpRequest req) {
        String email = normalizeEmail(req.email());
        assertEmailAvailable(email);
        otpService.issue(EmailOtp.Purpose.SIGNUP, email, "confirm your email and finish your workspace request", null);
    }

    @Transactional
    public SignupRequestResponse submit(SignupSubmitRequest req) {
        String email = normalizeEmail(req.email());
        assertEmailAvailable(email);
        otpService.verify(EmailOtp.Purpose.SIGNUP, email, req.otp());

        SignupRequest saved = signupRepo.save(SignupRequest.builder()
                .companyName(req.companyName().trim())
                .contactName(req.contactName().trim())
                .email(email)
                .phone(trimToNull(req.phone()))
                .industry(trimToNull(req.industry()))
                .websiteUrl(trimToNull(req.websiteUrl()))
                .message(trimToNull(req.message()))
                .status(SignupRequest.Status.PENDING)
                .build());

        notifyPlatformAdmins(saved);
        return SignupRequestResponse.from(saved);
    }

    private void assertEmailAvailable(String email) {
        emailUniquenessService.assertAccountEmailAvailable(email);
        if (signupRepo.existsByEmailIgnoreCaseAndStatus(email, SignupRequest.Status.PENDING)) {
            throw new BadRequestException("A signup request with this email is already awaiting review");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String s) {
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    /** Best-effort: a failed notification email must not fail the prospect's submission. */
    private void notifyPlatformAdmins(SignupRequest request) {
        List<PlatformAdmin> admins = platformAdminRepo.findAllByActiveTrue();
        for (PlatformAdmin admin : admins) {
            try {
                emailService.send(NotificationType.PLATFORM_SIGNUP_REQUEST, admin.getEmail(), Map.of(
                        "name", admin.getName(),
                        "companyName", request.getCompanyName(),
                        "contactName", request.getContactName(),
                        "contactEmail", request.getEmail(),
                        "message", Objects.requireNonNullElse(request.getMessage(), "")), null);
            } catch (RuntimeException ex) {
                log.warn("Failed to notify platform admin {} of signup request {}: {}",
                        admin.getEmail(), request.getId(), ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SignupRequestResponse> list() {
        return signupRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(SignupRequestResponse::from)
                .toList();
    }

    @Transactional
    public SignupRequestResponse reject(Long id) {
        SignupRequest request = signupRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Signup request not found"));
        if (request.getStatus() == SignupRequest.Status.APPROVED) {
            throw new BadRequestException("This request has already been approved");
        }
        request.setStatus(SignupRequest.Status.REJECTED);
        return SignupRequestResponse.from(signupRepo.save(request));
    }

    /** The raw signup request, if any — used to seed a newly provisioned org's profile. */
    @Transactional(readOnly = true)
    public java.util.Optional<SignupRequest> find(Long id) {
        return id == null ? java.util.Optional.empty() : signupRepo.findById(id);
    }

    /** Marks the request APPROVED after its organization has been provisioned. */
    @Transactional
    public void markApproved(Long id) {
        signupRepo.findById(id).ifPresent(request -> {
            request.setStatus(SignupRequest.Status.APPROVED);
            signupRepo.save(request);
        });
    }
}
