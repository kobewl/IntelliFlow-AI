package com.kobeai.hub.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("聊天请求")
public class ChatRequest {
    @ApiModelProperty(value = "聊天消息", required = true, example = "你好")
    private String message;
    private String platformType; // DEEPSEEK, CHATGPT, CLAUDE
    private Long conversationId; // 可选，用于继续对话
}