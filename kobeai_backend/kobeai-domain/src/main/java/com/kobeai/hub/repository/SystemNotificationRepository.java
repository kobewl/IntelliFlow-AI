package com.kobeai.hub.repository;

import com.kobeai.hub.model.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    // 查找所有活跃的通知
    List<SystemNotification> findByStatusOrderByCreatedAtDesc(String status);

    // 查找特定类型的通知
    List<SystemNotification> findByTypeAndStatusOrderByCreatedAtDesc(String type, String status);
}