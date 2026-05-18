package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.EmailPreviewRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.service.SendGridEmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Tag(name = "Public - Email Preview")
@RestController
@RequestMapping("/email-preview")
public class EmailPreviewController {
    private static final String DEFAULT_RECIPIENT_NAME = "AfroDebab User";
    private static final String SAMPLE_PASSWORD = "Temp#1234";
    private static final String SAMPLE_JOB_TITLE = "Frontend Developer";
    private static final String SAMPLE_TX_REF = "TX-EMAIL-PREVIEW-001";
    private static final long SAMPLE_AMOUNT = 25000L;

    private final SendGridEmailService sendGridEmailService;

    public EmailPreviewController(SendGridEmailService sendGridEmailService) {
        this.sendGridEmailService = sendGridEmailService;
    }

    @GetMapping("/cases")
    public List<String> listEmailCases() {
        return List.of(
                "EMPLOYEE_PASSWORD",
                "ADMIN_PAYROLL_REMINDER",
                "EMPLOYEE_PAYMENT_RECEIVED",
                "HIRING_SELECTED_FOR_INTERVIEW",
                "HIRING_REJECTED_PRE_INTERVIEW",
                "HIRING_HIRED",
                "HIRING_REJECTED_POST_INTERVIEW"
        );
    }

    @PostMapping("/send")
    public Map<String, String> sendPreview(@Valid @RequestBody EmailPreviewRequest req) {
        String emailCase = req.emailCase().trim().toUpperCase(Locale.ROOT);
        String email = req.email().trim();

        switch (emailCase) {
            case "EMPLOYEE_PASSWORD" -> sendGridEmailService.sendEmployeePasswordEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    SAMPLE_PASSWORD
            );
            case "ADMIN_PAYROLL_REMINDER" -> sendGridEmailService.sendAdminPayrollReminderEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    4
            );
            case "EMPLOYEE_PAYMENT_RECEIVED" -> sendGridEmailService.sendEmployeePaymentReceivedEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    SAMPLE_AMOUNT,
                    SAMPLE_TX_REF,
                    LocalDate.now()
            );
            case "HIRING_SELECTED_FOR_INTERVIEW" -> sendGridEmailService.sendCandidateSelectedForInterviewEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    SAMPLE_JOB_TITLE
            );
            case "HIRING_REJECTED_PRE_INTERVIEW" -> sendGridEmailService.sendCandidateRejectedBeforeInterviewEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    SAMPLE_JOB_TITLE
            );
            case "HIRING_HIRED" -> sendGridEmailService.sendCandidateHiredEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    SAMPLE_JOB_TITLE
            );
            case "HIRING_REJECTED_POST_INTERVIEW" -> sendGridEmailService.sendCandidateRejectedPostInterviewEmail(
                    email,
                    DEFAULT_RECIPIENT_NAME,
                    SAMPLE_JOB_TITLE
            );
            default -> throw new BadRequestException("Unsupported emailCase. Use GET /email-preview/cases");
        }

        return Map.of(
                "message", "Preview email sent",
                "email", email,
                "emailCase", emailCase
        );
    }
}

