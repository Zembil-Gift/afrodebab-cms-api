package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmailNotificationResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailNotification;
import com.afrodebab.cms.jpa.entity.EmailNotification.NotificationType;
import com.afrodebab.cms.jpa.repository.EmailNotificationRepository;
import com.afrodebab.cms.security.TokenCipher;
import com.afrodebab.cms.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailNotificationService {
    private static final int MAX_ATTEMPTS = 3;

    private final EmailNotificationRepository emailNotificationRepo;
    private final EmailTemplateService emailTemplateService;
    private final ObjectMapper objectMapper;
    private final TokenCipher tokenCipher;

    // Marks an encrypted password in a queued payload; sent payloads keep only REDACTED.
    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final String REDACTED = "[redacted]";
    private static final String PASSWORD = "password";
    private static final Map<String, String> LEGACY_KEYS = Map.of(
            "recipientName", "name",
            "generatedPassword", PASSWORD,
            "dueCount", "count",
            "paidAmountMinor", "amount",
            "transactionReference", "reference");

    public EmailNotificationService(EmailNotificationRepository emailNotificationRepo,
                                    EmailTemplateService emailTemplateService,
                                    ObjectMapper objectMapper,
                                    TokenCipher tokenCipher) {
        this.tokenCipher = tokenCipher;
        this.emailNotificationRepo = emailNotificationRepo;
        this.emailTemplateService = emailTemplateService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void queueEmployeePasswordEmail(String recipientEmail, String recipientName, String generatedPassword) {
        queue(NotificationType.EMPLOYEE_PASSWORD, recipientEmail,
                vars("name", recipientName, PASSWORD, protectPassword(generatedPassword)));
    }

    /** Manually added employees are one-offs, so their credentials skip the org's dispatch schedule; a failed send stays queued for it. */
    @Transactional
    public void sendEmployeePasswordEmailNow(String recipientEmail, String recipientName, String generatedPassword) {
        sendNotification(queue(NotificationType.EMPLOYEE_PASSWORD, recipientEmail,
                vars("name", recipientName, PASSWORD, protectPassword(generatedPassword))));
    }

    @Transactional
    public void queueEmployeeEmailChangedEmail(String recipientEmail, String recipientName, String generatedPassword) {
        queue(NotificationType.EMPLOYEE_EMAIL_CHANGED, recipientEmail,
                vars("name", recipientName, PASSWORD, protectPassword(generatedPassword)));
    }

    @Transactional
    public void queueAdminPayrollReminderEmail(String recipientEmail, String recipientName, int dueCount) {
        queue(NotificationType.ADMIN_PAYROLL_REMINDER, recipientEmail,
                vars("name", recipientName, "count", String.valueOf(dueCount)));
    }

    @Transactional
    public void queueViceManagerPayrollReminderEmail(String recipientEmail, String recipientName, String branch, int dueCount) {
        queue(NotificationType.VICE_MANAGER_PAYROLL_REMINDER, recipientEmail,
                vars("name", recipientName, "branch", branch, "count", String.valueOf(dueCount)));
    }

    @Transactional
    public void queueViceManagerNewEmployeeEmail(String recipientEmail, String recipientName, String branch,
                                                 String employeeName, String employeeEmail, String position) {
        queue(NotificationType.VICE_MANAGER_NEW_EMPLOYEE, recipientEmail,
                vars("name", recipientName, "branch", branch, "employeeName", employeeName,
                        "employeeEmail", employeeEmail, "position", position));
    }

    @Transactional
    public void queueManagerNewJobApplicationEmail(String recipientEmail, String recipientName,
                                                   String candidateName, String candidateEmail, String jobTitle) {
        queue(NotificationType.MANAGER_NEW_JOB_APPLICATION, recipientEmail,
                vars("name", recipientName, "candidateName", candidateName,
                        "candidateEmail", candidateEmail, "jobTitle", jobTitle));
    }

    @Transactional
    public void queueEmployeePaymentReceivedEmail(String recipientEmail,
                                                  String recipientName,
                                                  Long paidAmountMinor,
                                                  String transactionReference,
                                                  LocalDate dueDate) {
        queue(NotificationType.EMPLOYEE_PAYMENT_RECEIVED, recipientEmail,
                vars("name", recipientName, "amount", String.valueOf(paidAmountMinor),
                        "reference", transactionReference, "dueDate", String.valueOf(dueDate)));
    }

    @Transactional
    public void queueHiringSelectedForInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(NotificationType.HIRING_SELECTED_FOR_INTERVIEW, recipientEmail, vars("name", recipientName, "jobTitle", jobTitle));
    }

    @Transactional
    public void queueHiringRejectedPreInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(NotificationType.HIRING_REJECTED_PRE_INTERVIEW, recipientEmail, vars("name", recipientName, "jobTitle", jobTitle));
    }

    @Transactional
    public void queueHiringHiredEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(NotificationType.HIRING_HIRED, recipientEmail, vars("name", recipientName, "jobTitle", jobTitle));
    }

    @Transactional
    public void queueHiringRejectedPostInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(NotificationType.HIRING_REJECTED_POST_INTERVIEW, recipientEmail, vars("name", recipientName, "jobTitle", jobTitle));
    }

    @Transactional(readOnly = true)
    public Page<EmailNotificationResponse> listFailed(Pageable pageable) {
        return emailNotificationRepo.findAllByStatus(EmailNotification.DeliveryStatus.FAILED, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public EmailNotificationResponse retryFailedNow(Long notificationId) {
        EmailNotification notification = emailNotificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Email notification not found"));

        if (notification.getStatus() != EmailNotification.DeliveryStatus.FAILED) {
            throw new BadRequestException("Only FAILED notifications can be retried");
        }
        if (notification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BadRequestException("Retry limit reached");
        }

        sendNotification(notification);
        return toResponse(notification);
    }

    /** Sends the current organization's queued and failed emails. Run by {@link EmailScheduleService} at the org's dispatch time. */
    @Transactional
    public void dispatchPending() {
        emailNotificationRepo
                .findAllByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                        List.of(EmailNotification.DeliveryStatus.PENDING, EmailNotification.DeliveryStatus.FAILED),
                        MAX_ATTEMPTS
                )
                .forEach(this::sendNotification);
    }

    private EmailNotification queue(NotificationType type, String recipientEmail, Map<String, String> vars) {
        String serializedPayload;
        try {
            serializedPayload = objectMapper.writeValueAsString(vars);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize email payload", ex);
        }

        EmailNotification notification = new EmailNotification();
        notification.setType(type);
        notification.setStatus(EmailNotification.DeliveryStatus.PENDING);
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject(emailTemplateService.subject(type, withRecipient(vars, recipientEmail), TenantContext.get()));
        notification.setPayload(serializedPayload);
        return emailNotificationRepo.save(notification);
    }

    private void sendNotification(EmailNotification notification) {
        int nextAttempt = notification.getAttemptCount() + 1;
        notification.setAttemptCount(nextAttempt);

        try {
            Map<String, String> vars = withRecipient(readPayload(notification), notification.getRecipientEmail());
            if (vars.containsKey(PASSWORD)) vars.put(PASSWORD, revealPassword(vars.get(PASSWORD)));
            emailTemplateService.send(notification.getType(), notification.getRecipientEmail(), vars,
                    notification.getOrganizationId());
            redactPasswordAfterSend(notification);
            notification.setStatus(EmailNotification.DeliveryStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setLastError(null);
        } catch (RuntimeException ex) {
            notification.setStatus(EmailNotification.DeliveryStatus.FAILED);
            notification.setLastError(ex.getMessage());
        }

        emailNotificationRepo.save(notification);
    }

    // Passwords are only needed until the email goes out; encrypt them while queued.
    private String protectPassword(String password) {
        return tokenCipher.isConfigured() ? ENCRYPTED_PREFIX + tokenCipher.encrypt(password) : password;
    }

    private String revealPassword(String stored) {
        if (REDACTED.equals(stored)) {
            throw new BadRequestException("Password was already delivered and redacted; reset the password instead");
        }
        return stored.startsWith(ENCRYPTED_PREFIX) ? tokenCipher.decrypt(stored.substring(ENCRYPTED_PREFIX.length())) : stored;
    }

    private void redactPasswordAfterSend(EmailNotification notification) {
        Map<String, String> payload = readPayload(notification);
        if (!payload.containsKey(PASSWORD)) return;
        payload.put(PASSWORD, REDACTED);
        try {
            notification.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to redact email payload", ex);
        }
    }

    /** Payload vars, with keys from payloads queued before templates existed mapped to placeholder names. */
    private Map<String, String> readPayload(EmailNotification notification) {
        Map<String, Object> raw;
        try {
            raw = objectMapper.readValue(notification.getPayload(), new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to parse email payload for notification " + notification.getId(), ex);
        }
        Map<String, String> vars = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (value != null) vars.put(LEGACY_KEYS.getOrDefault(key, key), String.valueOf(value));
        });
        return vars;
    }

    private static Map<String, String> withRecipient(Map<String, String> vars, String recipientEmail) {
        Map<String, String> copy = new LinkedHashMap<>(vars);
        copy.putIfAbsent("email", recipientEmail);
        return copy;
    }

    private static Map<String, String> vars(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private EmailNotificationResponse toResponse(EmailNotification notification) {
        return new EmailNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getStatus(),
                notification.getRecipientEmail(),
                notification.getSubject(),
                notification.getAttemptCount(),
                notification.getLastError(),
                notification.getSentAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }

}
