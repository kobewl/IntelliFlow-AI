package com.kobeai.hub.service;

import com.kobeai.hub.dto.SystemNotificationDTO;
import com.kobeai.hub.model.SystemNotification;

import java.util.List;

public interface SystemNotificationService {
    // 创建新的系统通知
    SystemNotification createNotification(String title, String content, String type, Long adminId);

    // 获取所有活跃的通知
    List<SystemNotificationDTO> getAllActiveNotifications();

    // 获取特定类型的通知
    List<SystemNotificationDTO> getNotificationsByType(String type);

    // 更新通知状态
    SystemNotification updateNotificationStatus(Long notificationId, String status, Long adminId);

    // 删除通知
    void deleteNotification(Long notificationId, Long adminId);
}