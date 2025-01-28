package com.kobeai.hub.service;

import com.kobeai.hub.dto.response.ApiResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    ApiResponse<?> getConversations(String authHeader);

    ApiResponse<?> getConversationById(Long id, String authHeader);

    ApiResponse<?> getCurrentConversation(String authHeader);

    ApiResponse<?> createConversation(String authHeader);

    ApiResponse<?> deleteConversation(String authHeader);

    ApiResponse<?> clearCurrentConversation(String authHeader);

    ApiResponse<?> renameConversation(Long id, String title, String authHeader);

    SseEmitter sendMessage(String message, Long conversationId, String authHeader);

    ApiResponse<?> getConversationMessages(Long id, String cursor, Integer limit, String authHeader);

    ApiResponse<?> deleteConversationById(Long id, String authHeader);
}