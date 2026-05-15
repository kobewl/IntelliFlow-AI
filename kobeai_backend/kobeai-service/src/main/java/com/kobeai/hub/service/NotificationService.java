package com.kobeai.hub.service;

import com.kobeai.hub.dto.request.NotificationRequest;
import com.kobeai.hub.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<Notification> getNotificationList(String title, String type, Pageable pageable);

    Notification addNotification(NotificationRequest request);

    Notification updateNotification(Long id, NotificationRequest request);

    void deleteNotification(Long id);

    Notification updateNotificationStatus(Long id, String status);
}