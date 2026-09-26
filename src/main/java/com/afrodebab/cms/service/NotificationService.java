package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.NotificationResponse;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Notification;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.NotificationRepository;
import com.afrodebab.cms.util.SimpleMarkdown;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;

/**
 * In-app notifications for the logged-in employee or manager/vice manager. Every read and
 * write is bound to the caller resolved from the JWT, so nobody can see or mark another
 * person's notifications.
 */
@Service
public class NotificationService {

    /** Which kind of account the caller is; employees and managers live in different tables. */
    public enum Audience { EMPLOYEE, MANAGER }

    private final NotificationRepository notificationRepo;
    private final EmployeeRepository employeeRepo;
    private final ManagerRepository managerRepo;

    public NotificationService(NotificationRepository notificationRepo,
                               EmployeeRepository employeeRepo,
                               ManagerRepository managerRepo) {
        this.notificationRepo = notificationRepo;
        this.employeeRepo = employeeRepo;
        this.managerRepo = managerRepo;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Audience audience, Pageable pageable) {
        Long id = currentRecipientId(audience);
        Page<Notification> page = audience == Audience.EMPLOYEE
                ? notificationRepo.findAllByEmployeeIdOrderByCreatedAtDesc(id, pageable)
                : notificationRepo.findAllByManagerIdOrderByCreatedAtDesc(id, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Audience audience) {
        Long id = currentRecipientId(audience);
        return audience == Audience.EMPLOYEE
                ? notificationRepo.countByEmployeeIdAndReadAtIsNull(id)
                : notificationRepo.countByManagerIdAndReadAtIsNull(id);
    }

    @Transactional
    public NotificationResponse markRead(Audience audience, Long notificationId) {
        Long id = currentRecipientId(audience);
        Notification notification = (audience == Audience.EMPLOYEE
                ? notificationRepo.findByIdAndEmployeeId(notificationId, id)
                : notificationRepo.findByIdAndManagerId(notificationId, id))
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (notification.getReadAt() == null) notification.setReadAt(Instant.now());
        return toResponse(notificationRepo.save(notification));
    }

    @Transactional
    public void markAllRead(Audience audience) {
        Long id = currentRecipientId(audience);
        Instant now = Instant.now();
        if (audience == Audience.EMPLOYEE) notificationRepo.markAllReadForEmployee(id, now);
        else notificationRepo.markAllReadForManager(id, now);
    }

    /** Persists notifications built by other services (broadcasts, interview invitations). */
    @Transactional
    public void createAll(Collection<Notification> notifications) {
        notificationRepo.saveAll(notifications);
    }

    private Long currentRecipientId(Audience audience) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var id = audience == Audience.EMPLOYEE
                ? employeeRepo.findByEmailIgnoreCase(email).map(e -> e.getId())
                : managerRepo.findByEmailIgnoreCase(email).map(m -> m.getId());
        return id.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                SimpleMarkdown.toHtml(n.getBody()),
                n.getLink(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }
}
