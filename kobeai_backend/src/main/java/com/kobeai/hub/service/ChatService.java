package com.kobeai.hub.service;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.Message;
import com.kobeai.hub.model.User;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatService {
    ApiResponse<?> getConversations(String authHeader);

    ApiResponse<?> getConversationById(Long id, String authHeader);

    ApiResponse<?> getCurrentConversation(String authHeader);

    ApiResponse<?> createConversation(String authHeader);

    ApiResponse<?> deleteConversation(String authHeader);

    ApiResponse<?> clearCurrentConversation(String authHeader);

    ApiResponse<?> renameConversation(Long id, String title, String authHeader);

    /**
     * 发送消息并获取AI回复
     * 
     * @param conversationId 会话ID
     * @param content        消息内容
     * @param user           发送消息的用户
     * @return SSE发射器，用于流式返回AI回复
     */
    SseEmitter sendMessage(Long conversationId, String content, User user, String platformType);

    /**
     * 获取指定会话的最近消息
     * 
     * @param conversationId 会话ID
     * @param limit          消息数量限制
     * @return 消息列表
     */
    List<Message> getMessages(Long conversationId, int limit);

    /**
     * 获取指定时间点之前的消息
     * 
     * @param conversationId 会话ID
     * @param timestamp      时间戳
     * @param limit          消息数量限制
     * @return 消息列表
     */
    List<Message> getMessagesBefore(Long conversationId, LocalDateTime timestamp, int limit);

    /**
     * 删除会话及其所有消息
     * 
     * @param conversationId 会话ID
     */
    void deleteConversation(Long conversationId);

    ApiResponse<?> getConversationMessages(Long id, String cursor, Integer limit, String authHeader);

    ApiResponse<?> deleteConversationById(Long id, String authHeader);
}