package com.kobeai.hub.controller;

import com.kobeai.hub.dto.request.ChatRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.Message;
import com.kobeai.hub.service.DeepseekService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

@Api(tags = "测试接口")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final DeepseekService deepseekService;

    @ApiOperation("测试聊天功能")
    @PostMapping("/chat")
    public SseEmitter testChat(
            @ApiParam(value = "聊天请求", required = true) @RequestBody ChatRequest request) {
        Message testMessage = new Message();
        testMessage.setRole(Message.Role.ASSISTANT);
        testMessage.setContent("");
        testMessage.setCreatedAt(LocalDateTime.now());
        return deepseekService.sendMessage(request.getMessage(), testMessage);
    }

    @ApiOperation("测试服务是否正常运行")
    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("Service is running");
    }

    @ApiOperation("重新初始化 Deepseek 服务")
    @PostMapping("/reinitialize")
    public ApiResponse<String> reinitialize() {
        try {
            deepseekService.close();
            deepseekService.initialize();
            return ApiResponse.success("Service reinitialized successfully");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/deepseek")
    @ApiOperation("测试 Deepseek API")
    public SseEmitter testDeepseek(@RequestParam String message) {
        Message testMessage = new Message();
        testMessage.setRole(Message.Role.ASSISTANT);
        testMessage.setContent("");
        testMessage.setCreatedAt(LocalDateTime.now());
        return deepseekService.sendMessage(message, testMessage);
    }
}