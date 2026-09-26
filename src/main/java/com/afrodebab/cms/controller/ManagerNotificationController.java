package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.NotificationResponse;
import com.afrodebab.cms.service.NotificationService;
import com.afrodebab.cms.service.NotificationService.Audience;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Manager - Notifications")
@RestController
@RequestMapping({"/manager/notifications", "/vice-manager/notifications"})
public class ManagerNotificationController {

    private final NotificationService service;

    public ManagerNotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<NotificationResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(Audience.MANAGER, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(Audience.MANAGER));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return service.markRead(Audience.MANAGER, id);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        service.markAllRead(Audience.MANAGER);
    }
}
