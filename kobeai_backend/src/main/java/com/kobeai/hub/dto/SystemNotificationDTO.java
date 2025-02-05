package com.kobeai.hub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemNotificationDTO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}