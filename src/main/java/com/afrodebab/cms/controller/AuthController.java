package com.afrodebab.cms.controller;



import com.afrodebab.cms.dto.LoginRequest;
import com.afrodebab.cms.dto.LoginResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.service.JwtService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@Tag(name = "Manager - Auth")
@RestController
@RequestMapping("/manager/auth")
public class AuthController {

    private final JwtService jwtService;
    private final ManagerRepository managerRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService, ManagerRepository managerRepo, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.managerRepo = managerRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String normalizedEmail = req.email().trim().toLowerCase(Locale.ROOT);

        // Email is globally unique; resolve the manager (and their org) across all tenants
        // since no organization is in scope yet at login time.
        return TenantContext.callAsRoot(() -> {
            Manager manager = managerRepo.findByEmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() -> new BadRequestException("Invalid credentials"));
            if (!manager.isActive()) throw new BadRequestException("Manager account is inactive");
            if (!passwordEncoder.matches(req.password(), manager.getPasswordHash())) {
                throw new BadRequestException("Invalid credentials");
            }

            manager.setLastLoginAt(Instant.now());
            managerRepo.save(manager);

            String token = jwtService.generateToken(normalizedEmail, "MANAGER", manager.getOrganizationId());
            return new LoginResponse(token);
        });
    }
}
