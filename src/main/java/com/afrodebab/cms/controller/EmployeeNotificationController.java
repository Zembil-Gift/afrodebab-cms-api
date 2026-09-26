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

@Tag(name = "Employee - Notifications")
@RestController
@RequestMapping("/employee/me/notifications")
public class EmployeeNotificationController {

    private final NotificationService service;

    public EmployeeNotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<NotificationResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(Audience.EMPLOYEE, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(Audience.EMPLOYEE));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return service.markRead(Audience.EMPLOYEE, id);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        service.markAllRead(Audience.EMPLOYEE);
    }
}
