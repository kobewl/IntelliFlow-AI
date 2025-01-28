package com.kobeai.hub.service;

import com.kobeai.hub.model.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DeepseekService {
    void initialize();

    void close();

    SseEmitter sendMessage(String message, Message aiMessage);
}