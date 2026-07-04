package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.SignupRequestResponse;
import com.afrodebab.cms.service.SignupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Platform-admin review of self-serve signup requests. Restricted to ROLE_ADMIN by SecurityConfig. */
@Tag(name = "Platform Admin - Signup Requests")
@RestController
@RequestMapping("/admin/signup-requests")
public class SignupAdminController {

    private final SignupService service;

    public SignupAdminController(SignupService service) {
        this.service = service;
    }

    @GetMapping
    public List<SignupRequestResponse> list() {
        return service.list();
    }

    @PostMapping("/{id}/reject")
    public SignupRequestResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }
}
