package com.kobeai.hub.service.impl;

import com.kobeai.hub.constant.RedisKeyConstant;
import com.kobeai.hub.model.Message;
import com.kobeai.hub.model.Conversation;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Cacheable(value = "messages", key = "'conv:' + #conversationId + ':before:' + #timestamp")
    public List<Message> getMessagesBefore(Long conversationId, LocalDateTime timestamp, int limit) {
        log.debug("从数据库获取消息历史, conversationId: {}, timestamp: {}", conversationId, timestamp);
        return messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                conversationId, timestamp, PageRequest.of(0, limit));
    }

    @Override
    @Cacheable(value = "messages", key = "'conv:' + #conversationId + ':latest:' + #limit")
    public List<Message> getMessages(Long conversationId, int limit) {
        log.debug("从数据库获取最新消息, conversationId: {}, limit: {}", conversationId, limit);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    @CacheEvict(value = "messages", allEntries = true)
    public void deleteByConversationId(Long conversationId) {
        log.debug("删除会话消息, conversationId: {}", conversationId);
        // 使用软删除替代物理删除
        messageRepository.softDeleteByConversationId(conversationId);

        // 删除相关缓存
        String messagePattern = RedisKeyConstant.CHAT_MESSAGES_KEY + conversationId + "*";
        Set<String> keys = redisTemplate.keys(messagePattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "messages", allEntries = true)
    public void deleteMessage(Long messageId) {
        log.debug("删除单条消息, messageId: {}", messageId);
        // 使用软删除
        messageRepository.softDeleteMessage(messageId);

        // 清除相关缓存
        // 由于不知道消息所属会话，清除所有消息缓存
        Set<String> keys = redisTemplate.keys("messages::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    @Cacheable(value = "messages", key = "'conv:' + #conversation.id + ':page:' + #pageable.pageNumber")
    public List<Message> findByConversation(Conversation conversation, Pageable pageable) {
        log.debug("获取会话分页消息, conversationId: {}, page: {}", conversation.getId(), pageable.getPageNumber());
        return messageRepository.findByConversation(conversation, pageable);
    }

    @Override
    @CachePut(value = "messages", key = "'conv:' + #result.conversation.id + ':msg:' + #result.id")
    public Message saveMessage(Message message) {
        log.debug("保存新消息, conversationId: {}", message.getConversation().getId());
        // 确保新消息未被标记为删除
        message.setIsDeleted(false);

        // 如果是更新消息，保持更新时间为当前时间
        if (message.getId() != null) {
            message.setUpdatedAt(LocalDateTime.now());
        }

        Message savedMessage = messageRepository.save(message);

        // 清除相关的缓存
        String latestKey = "messages::conv:" + message.getConversation().getId() + ":latest:*";
        Set<String> keys = redisTemplate.keys(latestKey);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        return savedMessage;
    }
}