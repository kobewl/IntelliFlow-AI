package com.kobeai.hub.controller;

import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/email")
@RequiredArgsConstructor
@Api(tags = "邮箱验证码接口")
public class EmailController {

    private final UserService userService;

    @PostMapping("/send")
    @ApiOperation("发送邮箱验证码")
    public ApiResponse<?> sendEmailCode(@RequestBody EmailRequest request) {
        log.info("Sending email verification code to: {}", request.getEmail());
        try {
            return userService.sendEmailVerificationCode(request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email verification code: {}", e.getMessage(), e);
            return ApiResponse.error("发送验证码失败: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    @ApiOperation("验证邮箱验证码")
    public ApiResponse<?> verifyEmailCode(@RequestBody EmailVerifyRequest request) {
        log.info("Verifying email code for: {}", request.getEmail());
        try {
            return userService.verifyEmailCode(request.getEmail(), request.getCode());
        } catch (Exception e) {
            log.error("Failed to verify email code: {}", e.getMessage(), e);
            return ApiResponse.error("验证码验证失败: " + e.getMessage());
        }
    }
}

class EmailRequest {
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

class EmailVerifyRequest {
    private String email;
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}