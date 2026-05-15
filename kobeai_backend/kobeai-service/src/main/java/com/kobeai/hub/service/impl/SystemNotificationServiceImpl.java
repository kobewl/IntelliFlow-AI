package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.SystemNotificationDTO;
import com.kobeai.hub.exception.ResourceNotFoundException;
import com.kobeai.hub.model.SystemNotification;
import com.kobeai.hub.repository.SystemNotificationRepository;
import com.kobeai.hub.service.SystemNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemNotificationServiceImpl implements SystemNotificationService {

    private final SystemNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public SystemNotification createNotification(String title, String content, String type, Long adminId) {
        SystemNotification notification = new SystemNotification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setStatus("ACTIVE");
        notification.setCreatedBy(adminId);
        notification = notificationRepository.save(notification);

        // 通过WebSocket广播通知
        SystemNotificationDTO dto = convertToDTO(notification);
        messagingTemplate.convertAndSend("/topic/notifications", dto);

        return notification;
    }

    @Override
    public List<SystemNotificationDTO> getAllActiveNotifications() {
        return notificationRepository.findByStatusOrderByCreatedAtDesc("ACTIVE")
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemNotificationDTO> getNotificationsByType(String type) {
        return notificationRepository.findByTypeAndStatusOrderByCreatedAtDesc(type, "ACTIVE")
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SystemNotification updateNotificationStatus(Long notificationId, String status, Long adminId) {
        SystemNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setStatus(status);
        notification = notificationRepository.save(notification);

        // 通过WebSocket广播状态更新
        SystemNotificationDTO dto = convertToDTO(notification);
        messagingTemplate.convertAndSend("/topic/notifications/status", dto);

        return notification;
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long adminId) {
        SystemNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notification);

        // 通过WebSocket广播删除事件
        messagingTemplate.convertAndSend("/topic/notifications/delete", notificationId);
    }

    private SystemNotificationDTO convertToDTO(SystemNotification notification) {
        SystemNotificationDTO dto = new SystemNotificationDTO();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }
}