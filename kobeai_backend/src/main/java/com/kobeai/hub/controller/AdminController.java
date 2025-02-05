package com.kobeai.hub.controller;

import com.kobeai.hub.dto.request.NotificationRequest;
import com.kobeai.hub.dto.request.UserRequest;
import com.kobeai.hub.dto.request.UserUpdateRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.Notification;
import com.kobeai.hub.model.User;
import com.kobeai.hub.model.User.UserRole;
import com.kobeai.hub.service.NotificationService;
import com.kobeai.hub.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Api(tags = "管理员接口")
@Slf4j
public class AdminController {

    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/users")
    @ApiOperation("获取用户列表")
    public ApiResponse<?> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userService.getUserList(username, role, pageable);
            return ApiResponse.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/users")
    @ApiOperation("添加用户")
    public ApiResponse<?> addUser(@RequestBody UserRequest request) {
        try {
            User user = userService.addUser(request);
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("添加用户失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    @ApiOperation("更新用户信息")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        try {
            User user = userService.updateUser(id, convertToUpdateRequest(request));
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/users/{id}")
    @ApiOperation("删除用户")
    public ApiResponse<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/users/{id}/role")
    @ApiOperation("设置用户角色")
    public ApiResponse<?> setUserRole(@PathVariable Long id, @RequestBody UserRequest request) {
        try {
            User user = userService.setUserRole(id, request.getUserRole(), request.getMembershipEndTime());
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("设置用户角色失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/notifications")
    @ApiOperation("获取通知列表")
    public ApiResponse<?> getNotificationList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notifications = notificationService.getNotificationList(title, type, pageable);
            return ApiResponse.success(notifications);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/notifications")
    @ApiOperation("添加通知")
    public ApiResponse<?> addNotification(@RequestBody NotificationRequest request) {
        try {
            Notification notification = notificationService.addNotification(request);
            return ApiResponse.success(notification);
        } catch (Exception e) {
            log.error("添加通知失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/notifications/{id}")
    @ApiOperation("更新通知")
    public ApiResponse<?> updateNotification(@PathVariable Long id, @RequestBody NotificationRequest request) {
        try {
            Notification notification = notificationService.updateNotification(id, request);
            return ApiResponse.success(notification);
        } catch (Exception e) {
            log.error("更新通知失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/notifications/{id}")
    @ApiOperation("删除通知")
    public ApiResponse<?> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            log.error("删除通知失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/notifications/{id}/status")
    @ApiOperation("更新通知状态")
    public ApiResponse<?> updateNotificationStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Notification notification = notificationService.updateNotificationStatus(id, status);
            return ApiResponse.success(notification);
        } catch (Exception e) {
            log.error("更新通知状态失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    @ApiOperation("获取统计数据")
    public ApiResponse<?> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 获取总用户数
            long totalUsers = userService.count();
            stats.put("totalUsers", totalUsers);

            // 获取今日新增用户数
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            long todayUsers = userService.countByCreatedAtAfter(today);
            stats.put("todayUsers", todayUsers);

            // 获取会员用户数
            long vipUsers = userService.countByUserRole(UserRole.VIP);
            stats.put("vipUsers", vipUsers);

            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // 辅助方法：将 UserRequest 转换为 UserUpdateRequest
    private UserUpdateRequest convertToUpdateRequest(UserRequest request) {
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setUsername(request.getUsername());
        updateRequest.setEmail(request.getEmail());
        updateRequest.setPhone(request.getPhone());
        updateRequest.setAvatar(request.getAvatar());
        updateRequest.setUserRole(request.getUserRole());
        updateRequest.setMembershipEndTime(request.getMembershipEndTime());
        return updateRequest;
    }
}