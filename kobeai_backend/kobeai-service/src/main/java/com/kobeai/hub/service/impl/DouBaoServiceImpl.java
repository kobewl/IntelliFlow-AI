package com.kobeai.hub.service.impl;

import com.kobeai.hub.model.Message;
import com.kobeai.hub.service.AI.DouBaoService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class DouBaoServiceImpl implements DouBaoService {
    @Override
    public void close() {
        // todo:实现豆包平台的关闭发送消息逻辑

    }

    @Override
    public SseEmitter sendMessage(String message, Message aiMessage) {
        // todo:实现豆包平台的发送消息逻辑
        return null;
    }
}


