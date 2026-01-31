package com.afrodebab.cms.controller;



import com.afrodebab.cms.dto.LoginRequest;
import com.afrodebab.cms.dto.LoginResponse;
import com.afrodebab.cms.jpa.repository.AdminRepository;
import com.afrodebab.cms.service.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Tag(name = "Auth")
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final AdminRepository adminRepo;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, AdminRepository adminRepo) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.adminRepo = adminRepo;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        // update last_login_at
        adminRepo.findByEmail(req.email()).ifPresent(a -> {
            a.setLastLoginAt(Instant.now());
            adminRepo.save(a);
        });

        String token = jwtService.generateToken(req.email());
        return new LoginResponse(token);
    }
}

