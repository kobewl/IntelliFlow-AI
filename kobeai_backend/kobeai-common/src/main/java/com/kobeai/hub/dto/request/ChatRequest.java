package com.kobeai.hub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天请求")
public class ChatRequest {
    @Schema(description = "聊天消息", required = true, example = "你好")
    private String message;
    private String platformType; // DEEPSEEK, CHATGPT, CLAUDE
    private Long conversationId; // 可选，用于继续对话
}