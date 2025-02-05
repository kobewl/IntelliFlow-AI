package com.kobeai.hub.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class NotificationRequest {
    private String title;
    private String content;
    private String type;
    private List<String> targetUsers;
    private String status;
}