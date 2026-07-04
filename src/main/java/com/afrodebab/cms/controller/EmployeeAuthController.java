package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeeLoginRequest;
import com.afrodebab.cms.dto.LoginResponse;
import com.afrodebab.cms.service.EmployeeService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employee - Auth")
@RestController
@RequestMapping("/employee/auth")
public class EmployeeAuthController {
    private final EmployeeService service;

    public EmployeeAuthController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody EmployeeLoginRequest req) {
        // Resolve the employee by globally-unique email across all orgs; the login opens its
        // transactional session in root scope so tenant filtering doesn't hide the record.
        return TenantContext.callAsRoot(() -> service.login(req));
    }
}

