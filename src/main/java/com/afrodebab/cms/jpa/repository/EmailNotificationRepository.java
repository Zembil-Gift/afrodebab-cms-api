package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.EmailNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    Page<EmailNotification> findAllByStatus(EmailNotification.DeliveryStatus status, Pageable pageable);
    List<EmailNotification> findAllByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
            Collection<EmailNotification.DeliveryStatus> statuses,
            int attemptCount
    );
}
