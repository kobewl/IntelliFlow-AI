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
        messageRepository.deleteByConversationId(conversationId);

        // 删除相关缓存
        String messagePattern = RedisKeyConstant.CHAT_MESSAGES_KEY + conversationId + "*";
        Set<String> keys = redisTemplate.keys(messagePattern);
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
    @CachePut(value = "messages", key = "'conv:' + #result.conversationId + ':msg:' + #result.id")
    public Message saveMessage(Message message) {
        log.debug("保存新消息, conversationId: {}", message.getConversationId());
        Message savedMessage = messageRepository.save(message);

        // 清除相关的缓存
        String latestKey = "messages::conv:" + message.getConversationId() + ":latest:*";
        Set<String> keys = redisTemplate.keys(latestKey);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        return savedMessage;
    }
}