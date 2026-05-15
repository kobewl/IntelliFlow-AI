package com.kobeai.hub.controller;

import com.kobeai.hub.dto.CreateSystemNotificationRequest;
import com.kobeai.hub.dto.SystemNotificationDTO;
import com.kobeai.hub.model.SystemNotification;
import com.kobeai.hub.service.SystemNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class SystemNotificationController {

    private final SystemNotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemNotification> createNotification(
            @RequestBody CreateSystemNotificationRequest request,
            @AuthenticationPrincipal Long adminId) {
        return ResponseEntity.ok(notificationService.createNotification(
                request.getTitle(),
                request.getContent(),
                request.getType(),
                adminId));
    }

    @GetMapping
    public ResponseEntity<List<SystemNotificationDTO>> getAllActiveNotifications() {
        return ResponseEntity.ok(notificationService.getAllActiveNotifications());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<SystemNotificationDTO>> getNotificationsByType(
            @PathVariable String type) {
        return ResponseEntity.ok(notificationService.getNotificationsByType(type));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemNotification> updateNotificationStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal Long adminId) {
        return ResponseEntity.ok(notificationService.updateNotificationStatus(id, status, adminId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        notificationService.deleteNotification(id, adminId);
        return ResponseEntity.ok().build();
    }
}