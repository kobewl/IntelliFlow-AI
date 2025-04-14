package com.kobeai.hub.service.AI;

import com.kobeai.hub.model.Message;
import com.kobeai.hub.model.User;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DeepSeekService {
    void initialize();

    void close();

    SseEmitter sendMessage(String message, Message aiMessage, User user);
}