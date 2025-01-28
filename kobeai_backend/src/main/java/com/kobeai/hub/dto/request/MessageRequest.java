package com.kobeai.hub.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "消息请求")
public class MessageRequest {
    @ApiModelProperty(value = "消息内容", required = true)
    private String message;
}