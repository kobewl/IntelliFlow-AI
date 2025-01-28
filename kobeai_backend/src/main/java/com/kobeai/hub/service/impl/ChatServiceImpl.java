package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.*;
import com.kobeai.hub.repository.AIPlatformRepository;
import com.kobeai.hub.repository.ConversationRepository;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.ChatService;
import com.kobeai.hub.service.DeepseekService;
import com.kobeai.hub.service.UserService;
import com.kobeai.hub.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIPlatformRepository platformRepository;
    private final UserService userService;
    private final DeepseekService deepseekService;
    private final JwtUtil jwtUtil;

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        throw new RuntimeException("Invalid authorization header");
    }

    @Transactional
    public Conversation getOrCreateConversation(User user, String firstMessage) {
        try {
            return conversationRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .orElseGet(() -> {
                        // 获取DeepSeek平台
                        AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK)
                                .orElseThrow(() -> new RuntimeException("DeepSeek平台未配置，请先初始化平台"));

                        log.info("Creating new conversation for user {} with platform {}", user.getUsername(),
                                platform.getId());

                        Conversation newConversation = new Conversation();
                        newConversation.setUser(user);
                        newConversation.setCreatedAt(LocalDateTime.now());
                        newConversation.setPlatform(platform);

                        // 根据用户的第一条消息生成标题
                        String title = generateTitle(firstMessage);
                        newConversation.setTitle(title);

                        Conversation savedConversation = conversationRepository.save(newConversation);
                        log.info("Created new conversation with ID: {}, Title: {}", savedConversation.getId(), title);

                        return savedConversation;
                    });
        } catch (Exception e) {
            log.error("Failed to get or create conversation: {}", e.getMessage(), e);
            throw new RuntimeException("获取或创建会话失败: " + e.getMessage(), e);
        }
    }

    private String generateTitle(String message) {
        // 如果消息为空，使用默认标题
        if (message == null || message.trim().isEmpty()) {
            return "新对话";
        }

        // 如果消息太长，截取前20个字符作为标题
        String title = message.trim();
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }
        return title;
    }

    @Override
    @Transactional
    public SseEmitter sendMessage(String message, Long conversationId, String authHeader) {
        try {
            log.info("开始处理发送消息请求");
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            log.info("用户 {} 发送消息", username);

            User user = userService.getUserProfile(token);
            log.info("获取到用户信息: {}", user.getUsername());

            // 获取会话
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            // 验证会话所属
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权访问此会话");
            }

            log.info("使用会话: {}", conversation.getId());

            // 保存用户消息
            Message userMessage = new Message();
            userMessage.setConversation(conversation);
            userMessage.setContent(message);
            userMessage.setRole(Message.Role.USER);
            userMessage.setCreatedAt(LocalDateTime.now());
            messageRepository.save(userMessage);

            // 创建 SSE emitter
            SseEmitter emitter = new SseEmitter();

            // 创建 AI 消息
            Message aiMessage = new Message();
            aiMessage.setConversation(conversation);
            aiMessage.setRole(Message.Role.ASSISTANT);
            aiMessage.setCreatedAt(LocalDateTime.now());
            aiMessage = messageRepository.save(aiMessage);

            // 设置超时处理
            final Message finalAiMessage = aiMessage;
            emitter.onTimeout(() -> {
                log.error("消息处理超时: {}", conversation.getId());
                try {
                    finalAiMessage.setContent("Error: Request timed out");
                    messageRepository.save(finalAiMessage);
                    log.info("已保存超时错误消息");
                } catch (Exception e) {
                    log.error("保存超时错误消息失败: {}", e.getMessage());
                }
            });

            emitter.onError(ex -> {
                log.error("消息流处理错误: {}, 会话ID: {}", ex.getMessage(), conversation.getId());
                try {
                    finalAiMessage.setContent("Error: " + ex.getMessage());
                    messageRepository.save(finalAiMessage);
                    log.info("已保存错误消息");
                } catch (Exception e) {
                    log.error("保存错误消息失败: {}", e.getMessage());
                }
            });

            // 使用 DeepSeek 服务获取 AI 响应
            try {
                SseEmitter deepseekEmitter = deepseekService.sendMessage(message, aiMessage);
                deepseekEmitter.onCompletion(() -> {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("完成消息流失败: {}", e.getMessage());
                    }
                });

                deepseekEmitter.onTimeout(() -> {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("消息流超时处理失败: {}", e.getMessage());
                    }
                });

                deepseekEmitter.onError((ex) -> {
                    try {
                        emitter.completeWithError(ex);
                    } catch (Exception e) {
                        log.error("消息流错误处理失败: {}", e.getMessage());
                    }
                });

                return deepseekEmitter;
            } catch (Exception e) {
                log.error("调用 DeepSeek 服务失败: {}", e.getMessage());
                emitter.completeWithError(e);
                return emitter;
            }
        } catch (Exception e) {
            log.error("发送消息处理失败: {}", e.getMessage(), e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteConversation(String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);
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
    public ApiResponse<?> createConversation(String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取DeepSeek平台
            AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK)
                    .orElseThrow(() -> new RuntimeException("DeepSeek平台未配置，请先初始化平台"));

            log.info("Creating new conversation for user {} with platform {}", username, platform.getId());

            // 创建新会话
            Conversation newConversation = new Conversation();
            newConversation.setUser(user);
            newConversation.setCreatedAt(LocalDateTime.now());
            newConversation.setPlatform(platform);
            newConversation.setTitle("新对话");

            // 保存会话
            Conversation savedConversation = conversationRepository.save(newConversation);
            log.info("Created new conversation with ID: {}", savedConversation.getId());

            // 返回成功响应
            Map<String, Object> data = new HashMap<>();
            data.put("id", savedConversation.getId());
            data.put("title", savedConversation.getTitle());
            data.put("messages", savedConversation.getMessages());
            data.put("createdAt", savedConversation.getCreatedAt());
            data.put("updatedAt", savedConversation.getCreatedAt());

            return ApiResponse.success("创建成功", data);
        } catch (Exception e) {
            log.error("创建会话失败: {}", e.getMessage(), e);
            return ApiResponse.error("创建会话失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getConversationMessages(Long id, String cursor, Integer limit, String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取会话
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            // 验证用户权限
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权访问此会话");
            }

            // 设置默认分页大小
            if (limit == null || limit <= 0) {
                limit = 20;
            }

            // 获取消息列表
            List<Message> messages;
            boolean hasMore = false;
            String nextCursor = null;

            Pageable pageable = PageRequest.of(0, limit + 1);

            if (cursor == null) {
                // 第一页
                messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);
            } else {
                // 使用游标分页
                LocalDateTime cursorTime = LocalDateTime.parse(cursor);
                messages = messageRepository.findByConversationAndCreatedAtBeforeOrderByCreatedAtDesc(
                        conversation, cursorTime, pageable);
            }

            // 检查是否有更多消息
            if (messages.size() > limit) {
                hasMore = true;
                nextCursor = messages.get(limit).getCreatedAt().toString();
                messages = messages.subList(0, limit);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("messages", messages);
            result.put("hasMore", hasMore);
            if (hasMore) {
                result.put("nextCursor", nextCursor);
            }

            return ApiResponse.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取会话消息失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> renameConversation(Long id, String title, String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取会话
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            // 验证用户权限
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权修改此会话");
            }

            // 更新标题
            conversation.setTitle(title);
            conversationRepository.save(conversation);

            return ApiResponse.success("重命名成功", conversation);
        } catch (Exception e) {
            log.error("重命名会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> clearCurrentConversation(String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取当前会话
            Conversation conversation = conversationRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            // 清空会话消息
            messageRepository.deleteByConversation(conversation);

            // 重置会话标题
            conversation.setTitle("新对话");
            conversationRepository.save(conversation);

            return ApiResponse.success("清空成功");
        } catch (Exception e) {
            log.error("清空会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getConversations(String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取用户的所有会话
            List<Conversation> conversations = conversationRepository.findByUserOrderByCreatedAtDesc(user);

            // 如果用户没有会话，创建一个新的
            if (conversations.isEmpty()) {
                Conversation newConversation = new Conversation();
                newConversation.setUser(user);
                newConversation.setCreatedAt(LocalDateTime.now());
                newConversation.setTitle("新对话");

                // 获取DeepSeek平台
                AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK)
                        .orElseThrow(() -> new RuntimeException("DeepSeek平台未配置，请先初始化平台"));
                newConversation.setPlatform(platform);

                conversations.add(conversationRepository.save(newConversation));
            }

            return ApiResponse.success("获取成功", conversations);
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getConversationById(Long id, String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取指定会话
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            // 验证用户权限
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权访问此会话");
            }

            // 获取会话的消息列表
            Pageable pageable = PageRequest.of(0, 20);
            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);
            conversation.setMessages(messages);

            return ApiResponse.success("获取成功", conversation);
        } catch (Exception e) {
            log.error("获取会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> getCurrentConversation(String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取用户的最新会话
            Conversation conversation = conversationRepository.findFirstByUserOrderByCreatedAtDesc(user)
                    .orElseGet(() -> {
                        // 如果没有会话，创建一个新的
                        AIPlatform platform = platformRepository.findByType(Platform.DEEPSEEK)
                                .orElseThrow(() -> new RuntimeException("DeepSeek平台未配置，请先初始化平台"));

                        Conversation newConversation = new Conversation();
                        newConversation.setUser(user);
                        newConversation.setCreatedAt(LocalDateTime.now());
                        newConversation.setPlatform(platform);
                        newConversation.setTitle("新对话");

                        return conversationRepository.save(newConversation);
                    });

            // 获取会话的消息列表
            Pageable pageable = PageRequest.of(0, 20);
            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);
            conversation.setMessages(messages);

            return ApiResponse.success("获取成功", conversation);
        } catch (Exception e) {
            log.error("获取当前会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> deleteConversationById(Long id, String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtUtil.getUsernameFromToken(token);
            User user = userService.getUserProfile(token);

            // 获取会话
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));

            // 验证用户权限
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权删除此会话");
            }

            // 删除会话
            conversationRepository.delete(conversation);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}