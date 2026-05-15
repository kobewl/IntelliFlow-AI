package com.kobeai.hub.dto;

import lombok.Data;

@Data
public class CreateSystemNotificationRequest {
    private String title;
    private String content;
    private String type;
}