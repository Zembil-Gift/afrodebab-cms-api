package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.EmailNotification;
import com.afrodebab.cms.jpa.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Queries take the org explicitly so they also work from root-scoped sessions (the email dispatcher).
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByOrganizationIdAndType(Long organizationId, EmailNotification.NotificationType type);
    List<EmailTemplate> findAllByOrganizationId(Long organizationId);
}
