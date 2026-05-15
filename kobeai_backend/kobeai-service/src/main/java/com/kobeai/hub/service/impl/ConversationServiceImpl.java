package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.*;
import com.kobeai.hub.repository.AIPlatformRepository;
import com.kobeai.hub.repository.ConversationRepository;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.ConversationService;
import com.kobeai.hub.service.UserService;
import com.kobeai.hub.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIPlatformRepository platformRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        throw new RuntimeException("Invalid authorization header");
    }

    private User getUser(String authHeader) {
        return userService.getUserProfile(extractToken(authHeader));
    }

    @Override
    public ApiResponse<?> getConversations(String authHeader) {
        try {
            User user = getUser(authHeader);
            List<Conversation> conversations = conversationRepository.findByUserOrderByCreatedAtDesc(user);

            if (conversations.isEmpty()) {
                AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK)
                        .orElse(null);
                Conversation newConversation = new Conversation();
                newConversation.setUser(user);
                newConversation.setCreatedAt(LocalDateTime.now());
                newConversation.setTitle("新对话");
                if (platform != null) newConversation.setPlatform(platform);
                conversations.add(conversationRepository.save(newConversation));
            }

            return ApiResponse.success("获取成功", conversations);
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getCurrentConversation(String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .orElseGet(() -> {
                        AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK).orElse(null);
                        Conversation c = new Conversation();
                        c.setUser(user);
                        c.setCreatedAt(LocalDateTime.now());
                        c.setPlatform(platform);
                        c.setTitle("新对话");
                        return conversationRepository.save(c);
                    });

            Pageable pageable = PageRequest.of(0, 20);
            List<Message> messages = messageRepository.findByConversation(conversation, pageable);
            conversation.setMessages(messages);

            return ApiResponse.success("获取成功", conversation);
        } catch (Exception e) {
            log.error("获取当前会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getConversationById(Long id, String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权访问此会话");
            }

            Pageable pageable = PageRequest.of(0, 20);
            List<Message> messages = messageRepository.findByConversation(conversation, pageable);
            conversation.setMessages(messages);

            return ApiResponse.success("获取成功", conversation);
        } catch (Exception e) {
            log.error("获取会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> createConversation(String authHeader) {
        try {
            User user = getUser(authHeader);
            AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK).orElse(null);

            Conversation conversation = new Conversation();
            conversation.setUser(user);
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setPlatform(platform);
            conversation.setTitle("新对话");

            Conversation saved = conversationRepository.save(conversation);

            Map<String, Object> data = new HashMap<>();
            data.put("id", saved.getId());
            data.put("title", saved.getTitle());
            data.put("messages", List.of());
            data.put("createdAt", saved.getCreatedAt());
            data.put("updatedAt", saved.getCreatedAt());

            return ApiResponse.success("创建成功", data);
        } catch (Exception e) {
            log.error("创建会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteConversation(String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            conversationRepository.delete(conversation);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> clearCurrentConversation(String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            messageRepository.deleteByConversationId(conversation.getId());
            conversation.setTitle("新对话");
            conversationRepository.save(conversation);
            return ApiResponse.success("清空成功");
        } catch (Exception e) {
            log.error("清空会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> renameConversation(Long id, String title, String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权修改此会话");
            }
            conversation.setTitle(title);
            conversationRepository.save(conversation);
            return ApiResponse.success("重命名成功", conversation);
        } catch (Exception e) {
            log.error("重命名会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getConversationMessages(Long id, String cursor, Integer limit, String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权访问此会话");
            }

            if (limit == null || limit <= 0) limit = 20;
            Pageable pageable = PageRequest.of(0, limit + 1);

            List<Message> messages;
            if (cursor == null) {
                messages = messageRepository.findMessages(conversation.getId(), pageable);
            } else {
                LocalDateTime cursorTime = LocalDateTime.parse(cursor);
                messages = messageRepository.findMessagesBefore(conversation.getId(), cursorTime, pageable);
            }

            boolean hasMore = messages.size() > limit;
            String nextCursor = null;
            if (hasMore) {
                nextCursor = messages.get(limit).getCreatedAt().toString();
                messages = messages.subList(0, limit);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("messages", messages);
            result.put("hasMore", hasMore);
            if (hasMore) result.put("nextCursor", nextCursor);

            return ApiResponse.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取会话消息失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteConversationById(Long id, String authHeader) {
        try {
            User user = getUser(authHeader);
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权删除此会话");
            }
            messageRepository.deleteByConversationId(id);
            conversationRepository.deleteById(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
