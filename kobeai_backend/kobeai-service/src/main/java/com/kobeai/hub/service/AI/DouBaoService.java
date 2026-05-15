package com.kobeai.hub.service.AI;

import com.kobeai.hub.model.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DouBaoService {

    void close();

    SseEmitter sendMessage(String message, Message aiMessage);
}

