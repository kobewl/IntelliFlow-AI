package com.kobeai.hub.service;

import com.kobeai.hub.model.Message;
import com.kobeai.hub.model.Conversation;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {
    /**
     * 获取指定时间之前的消息
     */
    List<Message> getMessagesBefore(Long conversationId, LocalDateTime timestamp, int limit);

    /**
     * 获取最新消息
     */
    List<Message> getMessages(Long conversationId, int limit);

    /**
     * 根据会话ID删除消息
     */
    void deleteByConversationId(Long conversationId);

    /**
     * 根据会话查询消息
     */
    List<Message> findByConversation(Conversation conversation, Pageable pageable);

    /**
     * 保存消息
     */
    Message saveMessage(Message message);
}