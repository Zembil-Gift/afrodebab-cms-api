package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.ManagerChangePasswordRequest;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailOtp;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The logged-in manager's own account settings. Password changes require an emailed code. */
@Service
public class ManagerAccountService {

    private final ManagerRepository managerRepo;
    private final EmailOtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public ManagerAccountService(ManagerRepository managerRepo, EmailOtpService otpService, PasswordEncoder passwordEncoder) {
        this.managerRepo = managerRepo;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendPasswordChangeOtp(String managerEmail) {
        Manager manager = getByEmailOrThrow(managerEmail);
        otpService.issue(EmailOtp.Purpose.MANAGER_PASSWORD_CHANGE, manager.getEmail(),
                "confirm your password change", manager.getOrganizationId());
    }

    @Transactional
    public void changePassword(String managerEmail, ManagerChangePasswordRequest req) {
        Manager manager = getByEmailOrThrow(managerEmail);
        otpService.verify(EmailOtp.Purpose.MANAGER_PASSWORD_CHANGE, manager.getEmail(), req.otp());
        manager.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        managerRepo.save(manager);
    }

    private Manager getByEmailOrThrow(String email) {
        return managerRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Manager not found"));
    }
}
