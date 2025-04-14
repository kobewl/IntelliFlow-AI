package com.kobeai.hub.controller;

import com.kobeai.hub.constant.UserConstant;
import com.kobeai.hub.dto.request.ChatRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.AIPlatform;
import com.kobeai.hub.model.Platform;
import com.kobeai.hub.model.User;
import com.kobeai.hub.repository.AIPlatformRepository;
import com.kobeai.hub.service.ChatService;
import com.kobeai.hub.service.UserService;
import com.kobeai.hub.service.impl.ChatServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Api(tags = "聊天管理")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    
    @Autowired
    private AIPlatformRepository aiPlatformRepository;

    @GetMapping("/conversations")
    @ApiOperation(value = "获取所有会话")
    public ApiResponse<?> getConversations(@RequestHeader("Authorization") String authHeader) {
        return chatService.getConversations(authHeader);
    }

    @GetMapping("/conversations/current")
    @ApiOperation(value = "获取当前会话")
    public ApiResponse<?> getCurrentConversation(@RequestHeader("Authorization") String authHeader) {
        return chatService.getCurrentConversation(authHeader);
    }

    @GetMapping("/conversations/{id}")
    @ApiOperation(value = "获取特定会话")
    public ApiResponse<?> getConversationById(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return chatService.getConversationById(id, authHeader);
    }

    @PostMapping("/conversations")
    @ApiOperation(value = "创建新会话")
    public ApiResponse<?> createConversation(@RequestHeader("Authorization") String authHeader) {
        log.info("收到创建会话请求 - authHeader: {}",
                authHeader.substring(0, Math.min(authHeader.length(), 20)) + "...");
        try {
            ApiResponse<?> response = chatService.createConversation(authHeader);
            if (response.getCode() != 200) {
                log.error("创建会话失败 - response: {}", response);
                return response;
            }
            log.info("会话创建成功 - response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("创建会话失败: {}", e.getMessage(), e);
            return ApiResponse.error("创建会话失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/conversations")
    @ApiOperation(value = "删除所有会话")
    public ApiResponse<?> deleteConversation(@RequestHeader("Authorization") String authHeader) {
        return chatService.deleteConversation(authHeader);
    }

    @DeleteMapping("/conversations/current")
    @ApiOperation(value = "删除当前会话")
    public ApiResponse<?> clearCurrentConversation(@RequestHeader("Authorization") String authHeader) {
        return chatService.clearCurrentConversation(authHeader);
    }

    @DeleteMapping("/conversations/{id}")
    @ApiOperation(value = "删除特定会话")
    public ApiResponse<?> deleteConversationById(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return chatService.deleteConversationById(id, authHeader);
    }

    @PutMapping("/conversations/{id}/title")
    @ApiOperation(value = "重命名会话")
    public ApiResponse<?> renameConversation(@PathVariable Long id, @RequestParam String title,
            @RequestHeader("Authorization") String authHeader) {
        return chatService.renameConversation(id, title, authHeader);
    }

    @PostMapping("/completions")
    @ApiOperation(value = "发送消息", notes = "发送消息到AI助手并获取回复")
    public SseEmitter sendMessage(@RequestBody ChatRequest request,
            @RequestHeader("Authorization") String authHeader, @RequestParam(required = true) String platformType) {
        log.info("收到发送消息请求 - message: {}, conversationId: {}, authHeader: {}",
                request.getMessage(),
                request.getConversationId());
        try {
            // 获取当前用户
            User user = userService.getUserProfile(authHeader);

            // 尝试转换平台类型字符串为枚举
            Platform platform;
            try {
                platform = Platform.valueOf(platformType);
            } catch (IllegalArgumentException e) {
                log.error("无效的平台类型: {}", platformType);
                throw new RuntimeException("无效的平台类型: " + platformType);
            }
            
            // 查找平台
            List<AIPlatform> allPlatforms = aiPlatformRepository.findAll();
            
            // 过滤符合条件的平台（系统平台或用户自己的平台）
            Optional<AIPlatform> platformOpt = allPlatforms.stream()
                .filter(p -> p.getType().equals(platform) && 
                       (p.getUserId() == null || p.getUserId() == 0 || user.getId().equals(p.getUserId())))
                .findFirst();
                
            if (!platformOpt.isPresent()) {
                throw new RuntimeException("找不到可用的AI平台，或您没有权限使用该平台");
            }
            
            // 调用服务方法，注意参数顺序：conversationId, content, user
            SseEmitter emitter = chatService.sendMessage(request.getConversationId(), request.getMessage(), user, platformType);
            log.info("消息发送成功，返回SSE emitter");
            return emitter;
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/conversations/{id}/messages")
    @ApiOperation(value = "获取会话消息")
    public ApiResponse<?> getConversationMessages(@PathVariable Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestHeader("Authorization") String authHeader) {
        return chatService.getConversationMessages(id, cursor, limit, authHeader);
    }
}