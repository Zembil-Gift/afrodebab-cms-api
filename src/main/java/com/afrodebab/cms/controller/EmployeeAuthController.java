package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmployeeLoginRequest;
import com.afrodebab.cms.dto.LoginResponse;
import com.afrodebab.cms.service.EmployeeService;
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
        return service.login(req);
    }
}

