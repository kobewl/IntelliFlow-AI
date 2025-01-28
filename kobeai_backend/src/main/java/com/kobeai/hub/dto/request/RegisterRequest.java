package com.kobeai.hub.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户注册请求")
public class RegisterRequest {

    @ApiModelProperty(value = "用户名", required = true, example = "zhangsan")
    private String username;

    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;

    @ApiModelProperty(value = "邮箱", required = true, example = "zhangsan@example.com")
    private String email;

    @ApiModelProperty(value = "手机号", required = true, example = "13800138000")
    private String phone;
}