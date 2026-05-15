package com.kobeai.hub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "用户名", required = true, example = "zhangsan")
    private String username;

    @Schema(description = "密码", required = true, example = "123456")
    private String password;

    @Schema(description = "邮箱（可选）", required = false, example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号（可选）", required = false, example = "13800138000")
    private String phone;
}