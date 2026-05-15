package com.kobeai.hub.controller;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.service.ConversationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "会话管理")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/conversations")
    @Operation(summary = "获取所有会话")
    public ApiResponse<?> getConversations(@RequestHeader("Authorization") String authHeader) {
        return conversationService.getConversations(authHeader);
    }

    @GetMapping("/conversations/current")
    @Operation(summary = "获取当前会话")
    public ApiResponse<?> getCurrentConversation(@RequestHeader("Authorization") String authHeader) {
        return conversationService.getCurrentConversation(authHeader);
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "获取特定会话")
    public ApiResponse<?> getConversationById(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return conversationService.getConversationById(id, authHeader);
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建新会话")
    public ApiResponse<?> createConversation(@RequestHeader("Authorization") String authHeader) {
        return conversationService.createConversation(authHeader);
    }

    @DeleteMapping("/conversations")
    @Operation(summary = "删除所有会话")
    public ApiResponse<?> deleteConversation(@RequestHeader("Authorization") String authHeader) {
        return conversationService.deleteConversation(authHeader);
    }

    @DeleteMapping("/conversations/current")
    @Operation(summary = "清空当前会话")
    public ApiResponse<?> clearCurrentConversation(@RequestHeader("Authorization") String authHeader) {
        return conversationService.clearCurrentConversation(authHeader);
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "删除特定会话")
    public ApiResponse<?> deleteConversationById(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return conversationService.deleteConversationById(id, authHeader);
    }

    @PutMapping("/conversations/{id}/title")
    @Operation(summary = "重命名会话")
    public ApiResponse<?> renameConversation(@PathVariable Long id, @RequestParam String title,
            @RequestHeader("Authorization") String authHeader) {
        return conversationService.renameConversation(id, title, authHeader);
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "获取会话消息")
    public ApiResponse<?> getConversationMessages(@PathVariable Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestHeader("Authorization") String authHeader) {
        return conversationService.getConversationMessages(id, cursor, limit, authHeader);
    }
}
