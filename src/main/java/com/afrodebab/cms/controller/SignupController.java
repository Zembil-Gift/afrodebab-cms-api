package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.SignupRequestResponse;
import com.afrodebab.cms.dto.SignupSubmitRequest;
import com.afrodebab.cms.service.SignupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Public "Start free" endpoint. Deliberately NOT under {@code /public/**} (which is
 * org-slug scoped by PublicTenantFilter): a signup request exists before any organization.
 */
@Tag(name = "Public - Signup")
@RestController
@RequestMapping("/signup")
public class SignupController {

    private final SignupService service;

    public SignupController(SignupService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SignupRequestResponse submit(@Valid @RequestBody SignupSubmitRequest req) {
        return service.submit(req);
    }
}
