package com.afrodebab.cms.service;

import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.EmailNotification.NotificationType;
import com.afrodebab.cms.jpa.entity.EmailOtp;
import com.afrodebab.cms.jpa.repository.EmailOtpRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Issues and checks 6-digit email verification codes (signup, manager password change). */
@Service
public class EmailOtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration OTP_RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final EmailOtpRepository otpRepo;
    private final EmailTemplateService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailOtpService(EmailOtpRepository otpRepo, EmailTemplateService emailService, PasswordEncoder passwordEncoder) {
        this.otpRepo = otpRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Emails a fresh code, replacing any earlier one for this purpose and address.
     * {@code action} completes "Use the code below to ..." in the email; orgId picks the branding (null = platform).
     */
    @Transactional
    public void issue(EmailOtp.Purpose purpose, String email, String action, Long orgId) {
        EmailOtp otp = otpRepo.findByPurposeAndEmail(purpose, email)
                .orElseGet(() -> EmailOtp.builder().purpose(purpose).email(email).build());
        if (otp.getUpdatedAt() != null && otp.getUpdatedAt().plus(OTP_RESEND_COOLDOWN).isAfter(Instant.now())) {
            throw new BadRequestException("Please wait a minute before requesting another code");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(Instant.now().plus(OTP_TTL));
        otp.setAttempts(0);
        otpRepo.save(otp);
        emailService.send(NotificationType.VERIFICATION_CODE, email, Map.of(
                "email", email, "code", code, "action", action,
                "minutes", String.valueOf(OTP_TTL.toMinutes())), orgId);
    }

    /**
     * Consumes the code or throws. Own transaction so a wrong guess's attempt count is kept even
     * though the caller's transaction rolls back on the exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BadRequestException.class)
    public void verify(EmailOtp.Purpose purpose, String email, String code) {
        EmailOtp otp = otpRepo.findByPurposeAndEmail(purpose, email)
                .orElseThrow(() -> new BadRequestException("Request a verification code first"));
        if (otp.getExpiresAt().isBefore(Instant.now()) || otp.getAttempts() >= OTP_MAX_ATTEMPTS) {
            otpRepo.delete(otp);
            throw new BadRequestException("Verification code expired, please request a new one");
        }
        if (code == null || !passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepo.save(otp);
            throw new BadRequestException("Invalid verification code");
        }
        otpRepo.delete(otp);
    }
}
