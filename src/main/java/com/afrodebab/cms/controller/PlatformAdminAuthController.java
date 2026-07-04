package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.LoginRequest;
import com.afrodebab.cms.dto.LoginResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.PlatformAdmin;
import com.afrodebab.cms.jpa.repository.PlatformAdminRepository;
import com.afrodebab.cms.service.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

/**
 * Login for global platform operators. Issues a token with no orgId (org-independent).
 */
@Tag(name = "Platform Admin - Auth")
@RestController
@RequestMapping("/admin/auth")
public class PlatformAdminAuthController {

    private final JwtService jwtService;
    private final PlatformAdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminAuthController(JwtService jwtService, PlatformAdminRepository adminRepo, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String normalizedEmail = req.email().trim().toLowerCase(Locale.ROOT);

        PlatformAdmin admin = adminRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (!admin.isActive()) throw new BadRequestException("Platform admin account is inactive");
        if (!passwordEncoder.matches(req.password(), admin.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        admin.setLastLoginAt(Instant.now());
        adminRepo.save(admin);

        String token = jwtService.generateToken(normalizedEmail, "ADMIN", null);
        return new LoginResponse(token);
    }
}
