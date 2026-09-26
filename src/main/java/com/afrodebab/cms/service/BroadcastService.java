package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.BroadcastPreviewResponse;
import com.afrodebab.cms.dto.BroadcastRequest;
import com.afrodebab.cms.dto.BroadcastResponse;
import com.afrodebab.cms.dto.EmailPreviewResponse;
import com.afrodebab.cms.jpa.entity.Broadcast;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.Notification;
import com.afrodebab.cms.jpa.entity.SubOrganization;
import com.afrodebab.cms.jpa.repository.BroadcastRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.NotificationRepository;
import com.afrodebab.cms.jpa.repository.SubOrganizationRepository;
import com.afrodebab.cms.util.SimpleMarkdown;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Managers broadcast to everyone or chosen branches; vice managers only to their own branch.
 * Recipients are the active employees and vice managers of the targeted branches (never the
 * sender). Each gets an in-app notification and, if requested, an email that goes out on the
 * next scheduler tick (see {@link EmailNotificationService#dispatchImmediate()}).
 */
@Service
public class BroadcastService {

    private final BroadcastRepository broadcastRepo;
    private final NotificationRepository notificationRepo;
    private final EmployeeRepository employeeRepo;
    private final ManagerRepository managerRepo;
    private final SubOrganizationRepository subOrganizationRepo;
    private final SubOrganizationService subOrganizationService;
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final EmailTemplateService emailTemplateService;

    public BroadcastService(BroadcastRepository broadcastRepo,
                            NotificationRepository notificationRepo,
                            EmployeeRepository employeeRepo,
                            ManagerRepository managerRepo,
                            SubOrganizationRepository subOrganizationRepo,
                            SubOrganizationService subOrganizationService,
                            NotificationService notificationService,
                            EmailNotificationService emailNotificationService,
                            EmailTemplateService emailTemplateService) {
        this.broadcastRepo = broadcastRepo;
        this.notificationRepo = notificationRepo;
        this.employeeRepo = employeeRepo;
        this.managerRepo = managerRepo;
        this.subOrganizationRepo = subOrganizationRepo;
        this.subOrganizationService = subOrganizationService;
        this.notificationService = notificationService;
        this.emailNotificationService = emailNotificationService;
        this.emailTemplateService = emailTemplateService;
    }

    private record Recipient(Long employeeId, Long managerId, String name, String email) {}

    @Transactional(readOnly = true)
    public BroadcastPreviewResponse preview(BroadcastRequest req) {
        Manager sender = currentSender();
        int count = recipients(sender, targets(sender, req.subOrganizationIds())).size();
        EmailPreviewResponse email = emailTemplateService.previewBroadcast(req.subject().trim(), req.body(), sender.getName());
        return new BroadcastPreviewResponse(email.subject(), email.html(), SimpleMarkdown.toHtml(req.body()), count);
    }

    @Transactional(readOnly = true)
    public int recipientCount(List<Long> subOrganizationIds) {
        Manager sender = currentSender();
        return recipients(sender, targets(sender, subOrganizationIds)).size();
    }

    @Transactional
    public BroadcastResponse send(BroadcastRequest req) {
        Manager sender = currentSender();
        Set<Long> targets = targets(sender, req.subOrganizationIds());
        List<Recipient> recipients = recipients(sender, targets);
        String subject = req.subject().trim();

        Broadcast broadcast = new Broadcast();
        broadcast.setSenderManagerId(sender.getId());
        broadcast.setSenderName(sender.getName());
        broadcast.setSubject(subject);
        broadcast.setBody(req.body());
        broadcast.setSendEmail(req.sendEmail());
        broadcast.setSubOrganizationIds(new HashSet<>(targets));
        broadcast.setRecipientCount(recipients.size());
        broadcast = broadcastRepo.save(broadcast);

        Long broadcastId = broadcast.getId();
        notificationService.createAll(recipients.stream().map(r -> {
            Notification n = new Notification();
            n.setEmployeeId(r.employeeId());
            n.setManagerId(r.managerId());
            n.setType(Notification.Type.BROADCAST);
            n.setTitle(subject);
            n.setBody(req.body());
            n.setBroadcastId(broadcastId);
            return n;
        }).toList());

        if (req.sendEmail()) {
            recipients.forEach(r -> emailNotificationService.queueBroadcastEmail(
                    r.email(), r.name(), subject, req.body(), sender.getName()));
        }
        return toResponse(broadcast);
    }

    /** Managers see every broadcast in the org; vice managers see the ones they sent. */
    @Transactional(readOnly = true)
    public Page<BroadcastResponse> list(Pageable pageable) {
        Manager sender = currentSender();
        Page<Broadcast> page = isVice(sender)
                ? broadcastRepo.findAllBySenderManagerIdOrderByCreatedAtDesc(sender.getId(), pageable)
                : broadcastRepo.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::toResponse);
    }

    private Set<Long> targets(Manager sender, List<Long> requested) {
        if (isVice(sender)) {
            if (sender.getSubOrganization() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vice manager has no branch");
            }
            return Set.of(sender.getSubOrganization().getId());
        }
        Set<Long> ids = new TreeSet<>(requested == null ? List.of() : requested);
        ids.forEach(subOrganizationService::getEntityOrThrow); // must belong to this org
        return ids;
    }

    private List<Recipient> recipients(Manager sender, Set<Long> subOrgIds) {
        Stream<Employee> employees = subOrgIds.isEmpty()
                ? employeeRepo.findAllByActiveTrueOrderByNameAsc().stream()
                : subOrgIds.stream().flatMap(id -> employeeRepo.findAllByActiveTrueAndSubOrganizationIdOrderByNameAsc(id).stream());
        Stream<Manager> vices = subOrgIds.isEmpty()
                ? managerRepo.findAllByActiveTrueAndRole(Manager.ManagerRole.VICE_MANAGER).stream()
                : subOrgIds.stream().flatMap(id -> managerRepo
                        .findAllByActiveTrueAndRoleAndSubOrganizationId(Manager.ManagerRole.VICE_MANAGER, id).stream());
        return Stream.concat(
                employees.map(e -> new Recipient(e.getId(), null, e.getName(), e.getEmail())),
                vices.filter(m -> !m.getId().equals(sender.getId()))
                        .map(m -> new Recipient(null, m.getId(), m.getName(), m.getEmail()))
        ).toList();
    }

    private Manager currentSender() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return managerRepo.findWithSubOrganizationByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private static boolean isVice(Manager manager) {
        return manager.getRole() == Manager.ManagerRole.VICE_MANAGER;
    }

    private BroadcastResponse toResponse(Broadcast b) {
        return new BroadcastResponse(
                b.getId(),
                b.getSubject(),
                b.getBody(),
                SimpleMarkdown.toHtml(b.getBody()),
                b.getSenderName(),
                subOrganizationRefs(b.getSubOrganizationIds()),
                b.isSendEmail(),
                b.getRecipientCount(),
                notificationRepo.countByBroadcastIdAndReadAtIsNotNull(b.getId()),
                b.getCreatedAt()
        );
    }

    private List<BroadcastResponse.SubOrganizationRef> subOrganizationRefs(Collection<Long> ids) {
        return subOrganizationRepo.findAllById(ids).stream()
                .sorted(Comparator.comparing(SubOrganization::getName))
                .map(s -> new BroadcastResponse.SubOrganizationRef(s.getId(), s.getName()))
                .toList();
    }
}
