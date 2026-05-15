package com.kobeai.hub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "消息请求")
public class MessageRequest {
    @Schema(description = "消息内容", required = true)
    private String message;
}