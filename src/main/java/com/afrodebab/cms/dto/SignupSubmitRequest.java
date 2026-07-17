package com.afrodebab.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public "Start free" submission from the marketing site. No slug or password: a platform
 *  admin sets the slug at approval and the manager password is generated then. */
public record SignupSubmitRequest(
        @NotBlank(message = "company name is required") @Size(max = 200) String companyName,
        @NotBlank(message = "your name is required") @Size(max = 150) String contactName,
        @Email(message = "a valid email is required") @NotBlank(message = "email is required") @Size(max = 200) String email,
        @Size(max = 60) String phone,
        @Size(max = 120) String industry,
        @Size(max = 512) String websiteUrl,
        @Size(max = 2000) String message
) {}
