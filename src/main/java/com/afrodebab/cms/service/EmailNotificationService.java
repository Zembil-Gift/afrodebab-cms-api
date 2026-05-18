package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmailNotificationResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailNotification;
import com.afrodebab.cms.jpa.repository.EmailNotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class EmailNotificationService {
    private static final int MAX_ATTEMPTS = 3;

    private final EmailNotificationRepository emailNotificationRepo;
    private final SendGridEmailService sendGridEmailService;
    private final ObjectMapper objectMapper;

    public EmailNotificationService(EmailNotificationRepository emailNotificationRepo,
                                    SendGridEmailService sendGridEmailService,
                                    ObjectMapper objectMapper) {
        this.emailNotificationRepo = emailNotificationRepo;
        this.sendGridEmailService = sendGridEmailService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void queueEmployeePasswordEmail(String recipientEmail, String recipientName, String generatedPassword) {
        queue(
                EmailNotification.NotificationType.EMPLOYEE_PASSWORD,
                recipientEmail,
                "Your AfroDebab employee account",
                new EmployeePasswordPayload(recipientName, generatedPassword)
        );
    }

    @Transactional
    public void queueAdminPayrollReminderEmail(String recipientEmail, String recipientName, int dueCount) {
        queue(
                EmailNotification.NotificationType.ADMIN_PAYROLL_REMINDER,
                recipientEmail,
                "AfroDebab payroll reminder",
                new AdminPayrollReminderPayload(recipientName, dueCount)
        );
    }

    @Transactional
    public void queueEmployeePaymentReceivedEmail(String recipientEmail,
                                                  String recipientName,
                                                  Long paidAmountMinor,
                                                  String transactionReference,
                                                  LocalDate dueDate) {
        queue(
                EmailNotification.NotificationType.EMPLOYEE_PAYMENT_RECEIVED,
                recipientEmail,
                "AfroDebab salary payment received",
                new EmployeePaymentReceivedPayload(recipientName, paidAmountMinor, transactionReference, dueDate)
        );
    }

    @Transactional
    public void queueHiringSelectedForInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(
                EmailNotification.NotificationType.HIRING_SELECTED_FOR_INTERVIEW,
                recipientEmail,
                "Interview selection - AfroDebab",
                new HiringPayload(recipientName, jobTitle)
        );
    }

    @Transactional
    public void queueHiringRejectedPreInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(
                EmailNotification.NotificationType.HIRING_REJECTED_PRE_INTERVIEW,
                recipientEmail,
                "Application update - AfroDebab",
                new HiringPayload(recipientName, jobTitle)
        );
    }

    @Transactional
    public void queueHiringHiredEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(
                EmailNotification.NotificationType.HIRING_HIRED,
                recipientEmail,
                "Offer update - AfroDebab",
                new HiringPayload(recipientName, jobTitle)
        );
    }

    @Transactional
    public void queueHiringRejectedPostInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        queue(
                EmailNotification.NotificationType.HIRING_REJECTED_POST_INTERVIEW,
                recipientEmail,
                "Interview result - AfroDebab",
                new HiringPayload(recipientName, jobTitle)
        );
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

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void dispatchDaily() {
        List<EmailNotification> notifications = emailNotificationRepo
                .findAllByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
                        List.of(EmailNotification.DeliveryStatus.PENDING, EmailNotification.DeliveryStatus.FAILED),
                        MAX_ATTEMPTS
                );

        for (EmailNotification notification : notifications) {
            sendNotification(notification);
        }
    }

    private void queue(EmailNotification.NotificationType type,
                       String recipientEmail,
                       String subject,
                       Object payload) {
        String serializedPayload;
        try {
            serializedPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize email payload", ex);
        }

        EmailNotification notification = new EmailNotification();
        notification.setType(type);
        notification.setStatus(EmailNotification.DeliveryStatus.PENDING);
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject(subject);
        notification.setPayload(serializedPayload);
        emailNotificationRepo.save(notification);
    }

    private void sendNotification(EmailNotification notification) {
        int nextAttempt = notification.getAttemptCount() + 1;
        notification.setAttemptCount(nextAttempt);

        try {
            sendByType(notification);
            notification.setStatus(EmailNotification.DeliveryStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setLastError(null);
        } catch (RuntimeException ex) {
            notification.setStatus(EmailNotification.DeliveryStatus.FAILED);
            notification.setLastError(ex.getMessage());
        }

        emailNotificationRepo.save(notification);
    }

    private void sendByType(EmailNotification notification) {
        switch (notification.getType()) {
            case EMPLOYEE_PASSWORD -> {
                EmployeePasswordPayload payload = readPayload(notification, EmployeePasswordPayload.class);
                sendGridEmailService.sendEmployeePasswordEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.generatedPassword()
                );
            }
            case ADMIN_PAYROLL_REMINDER -> {
                AdminPayrollReminderPayload payload = readPayload(notification, AdminPayrollReminderPayload.class);
                sendGridEmailService.sendAdminPayrollReminderEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.dueCount()
                );
            }
            case EMPLOYEE_PAYMENT_RECEIVED -> {
                EmployeePaymentReceivedPayload payload = readPayload(notification, EmployeePaymentReceivedPayload.class);
                sendGridEmailService.sendEmployeePaymentReceivedEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.paidAmountMinor(),
                        payload.transactionReference(),
                        payload.dueDate()
                );
            }
            case HIRING_SELECTED_FOR_INTERVIEW -> {
                HiringPayload payload = readPayload(notification, HiringPayload.class);
                sendGridEmailService.sendCandidateSelectedForInterviewEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.jobTitle()
                );
            }
            case HIRING_REJECTED_PRE_INTERVIEW -> {
                HiringPayload payload = readPayload(notification, HiringPayload.class);
                sendGridEmailService.sendCandidateRejectedBeforeInterviewEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.jobTitle()
                );
            }
            case HIRING_HIRED -> {
                HiringPayload payload = readPayload(notification, HiringPayload.class);
                sendGridEmailService.sendCandidateHiredEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.jobTitle()
                );
            }
            case HIRING_REJECTED_POST_INTERVIEW -> {
                HiringPayload payload = readPayload(notification, HiringPayload.class);
                sendGridEmailService.sendCandidateRejectedPostInterviewEmail(
                        notification.getRecipientEmail(),
                        payload.recipientName(),
                        payload.jobTitle()
                );
            }
        }
    }

    private <T> T readPayload(EmailNotification notification, Class<T> payloadType) {
        try {
            return objectMapper.readValue(notification.getPayload(), payloadType);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to parse email payload for notification " + notification.getId(), ex);
        }
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

    private record EmployeePasswordPayload(String recipientName, String generatedPassword) {}
    private record AdminPayrollReminderPayload(String recipientName, int dueCount) {}
    private record EmployeePaymentReceivedPayload(String recipientName,
                                                  Long paidAmountMinor,
                                                  String transactionReference,
                                                  LocalDate dueDate) {}
    private record HiringPayload(String recipientName, String jobTitle) {}
}
