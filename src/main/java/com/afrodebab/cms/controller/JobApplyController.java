package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.ApplyRequest;
import com.afrodebab.cms.dto.ApplyResponse;
import com.afrodebab.cms.service.JobApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public - JobApply")
@Validated
@RestController
@RequestMapping("/jobs")
public class JobApplyController {

    private final JobApplicationService service;

    public JobApplyController(JobApplicationService service) { this.service = service; }

    @PostMapping("/{id}/apply")
    public ApplyResponse apply(@PathVariable Long id, @Valid @RequestBody ApplyRequest req) {
        return service.apply(id, req);
    }

    @PostMapping(value = "/{id}/apply/form", consumes = "multipart/form-data")
    public ApplyResponse applyWithResume(@PathVariable Long id,
                                         @RequestParam @NotBlank(message = "fullName is required") String fullName,
                                         @RequestParam @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
                                         @RequestParam(required = false) String phoneNumber,
                                         @RequestParam(required = false) String githubUrl,
                                         @RequestParam(name = "resume") MultipartFile resume) {
        ApplyRequest req = new ApplyRequest(fullName, email, phoneNumber, githubUrl);
        return service.applyWithResume(id, req, resume);
    }
}
