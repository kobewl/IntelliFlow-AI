package com.kobeai.hub.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIPlatformService {
    void initialize();

    void close();

    SseEmitter sendMessage(String message);
}