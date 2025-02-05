package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.request.NotificationRequest;
import com.kobeai.hub.model.Notification;
import com.kobeai.hub.repository.NotificationRepository;
import com.kobeai.hub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<Notification> getNotificationList(String title, String type, Pageable pageable) {
        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title + "%"));
            }

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return notificationRepository.findAll(spec, pageable);
    }

    @Override
    public Notification addNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setType(request.getType());
        notification.setTargetUsers(request.getTargetUsers());
        notification.setStatus(request.getStatus());
        return notificationRepository.save(notification);
    }

    @Override
    public Notification updateNotification(Long id, NotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setType(request.getType());
        notification.setTargetUsers(request.getTargetUsers());
        notification.setStatus(request.getStatus());

        return notificationRepository.save(notification);
    }

    @Override
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public Notification updateNotificationStatus(Long id, String status) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        notification.setStatus(status);
        return notificationRepository.save(notification);
    }
}