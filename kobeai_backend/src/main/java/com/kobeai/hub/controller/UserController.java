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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Api(tags = "用户管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
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
            @RequestParam("userId") Long userId) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.error("请选择要上传的文件");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                return ApiResponse.error("只支持 JPG/PNG 格式的图片");
            }

            // 检查文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ApiResponse.error("文件大小不能超过5MB");
            }

            // 上传文件到MinIO
            String avatarUrl = fileService.uploadFile(file, "photo");

            // 更新用户头像
            return userService.updateAvatar(userId, avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败: {}", e.getMessage(), e);
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

    @GetMapping("/uploads/avatars/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/avatars").resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        throw new RuntimeException("Invalid authorization header");
    }
}