package com.kobeai.hub.controller;

import com.kobeai.hub.dto.request.ChangePasswordRequest;
import com.kobeai.hub.dto.request.LoginRequest;
import com.kobeai.hub.dto.request.RegisterRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.User;
import com.kobeai.hub.service.FileService;
import com.kobeai.hub.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Api(tags = "用户认证")
public class UserController {

    private final UserService userService;
    private final FileService fileService;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }

    @GetMapping("/profile")
    @ApiOperation("获取用户信息")
    public ApiResponse<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            return ApiResponse.success(userService.getUserProfile(token));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public ApiResponse<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            userService.logout(token);
            return ApiResponse.success("登出成功");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/avatar")
    @ApiOperation("上传用户头像")
    public ApiResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            User user = userService.getUserProfile(token);

            // 上传文件到 MinIO
            String avatarUrl = fileService.uploadFile(file, "photo");

            // 更新用户头像
            return userService.updateAvatar(user.getId(), avatarUrl);
        } catch (Exception e) {
            return ApiResponse.error("头像上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    @ApiOperation("更新用户信息")
    public ApiResponse<?> updateProfile(@RequestBody User user,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            User currentUser = userService.getUserProfile(token);
            user.setId(currentUser.getId()); // 确保使用当前用户的ID
            return userService.updateProfile(user);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/password")
    @ApiOperation("修改密码")
    public ApiResponse<?> changePassword(@RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            User currentUser = userService.getUserProfile(token);
            return userService.changePassword(currentUser.getId(), request.getCurrentPassword(),
                    request.getNewPassword());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        throw new RuntimeException("Invalid authorization header");
    }
}