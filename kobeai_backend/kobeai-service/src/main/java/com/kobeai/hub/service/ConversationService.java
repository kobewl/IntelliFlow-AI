package com.kobeai.hub.service;

import com.kobeai.hub.dto.response.ApiResponse;

public interface ConversationService {
    ApiResponse<?> getConversations(String authHeader);
    ApiResponse<?> getCurrentConversation(String authHeader);
    ApiResponse<?> getConversationById(Long id, String authHeader);
    ApiResponse<?> createConversation(String authHeader);
    ApiResponse<?> deleteConversation(String authHeader);
    ApiResponse<?> clearCurrentConversation(String authHeader);
    ApiResponse<?> renameConversation(Long id, String title, String authHeader);
    ApiResponse<?> getConversationMessages(Long id, String cursor, Integer limit, String authHeader);
    ApiResponse<?> deleteConversationById(Long id, String authHeader);
}
