package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.*;
import com.kobeai.hub.repository.AIPlatformRepository;
import com.kobeai.hub.repository.ConversationRepository;
import com.kobeai.hub.repository.MessageRepository;
import com.kobeai.hub.service.AI.DouBaoService;
import com.kobeai.hub.service.ChatService;
import com.kobeai.hub.service.AI.DeepSeekService;
import com.kobeai.hub.service.UserService;
import com.kobeai.hub.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final DeepSeekService deepseekService;
    private final DouBaoService doubaoService;
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
    public SseEmitter sendMessage(Long conversationId, String content, User user, String platformType) {
        // 获取或创建会话
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseGet(() -> {
                    Conversation newConversation = new Conversation();
                    newConversation.setUser(user);
                    newConversation.setCreatedAt(LocalDateTime.now());
                    return conversationRepository.save(newConversation);
                });

        // 创建用户消息
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setSenderId(user.getId());
        userMessage.setRole(Message.Role.USER);
        userMessage.setContent(content);
        userMessage.setCreatedAt(LocalDateTime.now());
        messageRepository.save(userMessage);

        // 检查是否是第一条消息，如果是则更新对话标题
        List<Message> existingMessages = messageRepository.findMessages(conversation.getId(), PageRequest.of(0, 1));
        if (existingMessages.size() == 1) { // 只有一条消息说明是第一次发送
            String title = generateTitleFromMessage(content);
            conversation.setTitle(title);
            conversationRepository.save(conversation);
        }

        // 创建AI回复消息
        Message aiMessage = new Message();
        aiMessage.setConversation(conversation);
        aiMessage.setSenderId(-1L); // 使用 -1 作为 AI 消息的发送者ID
        aiMessage.setRole(Message.Role.ASSISTANT);
        aiMessage.setContent("");
        aiMessage.setCreatedAt(LocalDateTime.now());
        messageRepository.save(aiMessage);

        // 获取历史消息用于上下文
        List<Message> historyMessages = messageRepository.findRecentMessages(conversation.getId(), 10);

        // 根据platformType调用对应的AI服务生成回复
        Platform platform;
        try {
            platform = Platform.valueOf(platformType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("无效的平台类型: {}", platformType);
            throw new RuntimeException("无效的平台类型: " + platformType);
        }
        
        // 根据不同的平台类型调用不同的服务实现
        switch (platform) {
            case DEEPSEEK:
                return deepseekService.sendMessage(content, aiMessage, user);
            case CHATGPT:
                // todo:如果后续实现了ChatGPT服务，可以在这里调用
                // return chatgptService.sendMessage(content, aiMessage);
                throw new RuntimeException("ChatGPT服务尚未实现");
            case CLAUDE:
                // todo:如果后续实现了Claude服务，可以在这里调用
                // return claudeService.sendMessage(content, aiMessage);
                throw new RuntimeException("Claude服务尚未实现");
            case BAIDU_WENXIN:
                // todo:如果后续实现了百度文心一言服务，可以在这里调用
                // return baiduWenxinService.sendMessage(content, aiMessage);
                throw new RuntimeException("百度文心一言服务尚未实现");
            case DOUBAO:
                 return doubaoService.sendMessage(content, aiMessage);
            default:
                log.error("不支持的AI平台类型: {}", platform);
                throw new RuntimeException("不支持的AI平台类型: " + platform);
        }
    }

    // 根据消息内容生成标题
    private String generateTitleFromMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "新对话";
        }

        // 去除换行符和多余空格
        String title = message.replaceAll("\\s+", " ").trim();

        // 如果内容超过10个字符，截取前10个字符
        if (title.length() > 10) {
            title = title.substring(0, 10) + "...";
        }

        return title;
    }

    @Override
    public List<Message> getMessages(Long conversationId, int limit) {
        return messageRepository.findRecentMessages(conversationId, limit);
    }

    @Override
    public List<Message> getMessagesBefore(Long conversationId, LocalDateTime timestamp, int limit) {
        return messageRepository.findMessagesBefore(
                conversationId, timestamp, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) {
        // 先删除所有相关消息
        messageRepository.deleteByConversationId(conversationId);
        // 再删除会话
        conversationRepository.deleteById(conversationId);
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
                messages = messageRepository.findMessages(conversation.getId(), pageable);
            } else {
                // 使用游标分页
                LocalDateTime cursorTime = LocalDateTime.parse(cursor);
                messages = messageRepository.findMessagesBefore(conversation.getId(), cursorTime, pageable);
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
            messageRepository.deleteByConversationId(conversation.getId());

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
            List<Message> messages = messageRepository.findByConversation(conversation, pageable);
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
            List<Message> messages = messageRepository.findByConversation(conversation, pageable);
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

            // 先删除该会话下所有相关消息，避免因 foreign key constraint 导致更新为 null 的错误
            messageRepository.deleteByConversationId(id);
            // 直接通过 ID 删除会话，避免 orphanRemoval 导致更新 conversation_id 为 null
            conversationRepository.deleteById(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
}