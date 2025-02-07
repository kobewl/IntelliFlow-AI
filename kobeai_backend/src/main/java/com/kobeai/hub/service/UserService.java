package com.kobeai.hub.service;

import com.kobeai.hub.dto.UserDTO;
import com.kobeai.hub.dto.request.RegisterRequest;
import com.kobeai.hub.dto.request.UserRequest;
import com.kobeai.hub.dto.request.UserUpdateRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.User;
import com.kobeai.hub.model.User.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface UserService {
    ApiResponse<?> register(RegisterRequest request);

    ApiResponse<?> login(String username, String password);

    User getUserProfile(String token);

    void logout(String token);

    // 更新用户角色
    ApiResponse<?> updateUserRole(Long userId, UserRole newRole);

    // 更新会员时间
    ApiResponse<?> updateMembership(Long userId, UserRole membershipType, int monthsDuration);

    // 检查会员状态
    boolean isActiveMember(Long userId);

    // 更新用户头像
    ApiResponse<?> updateAvatar(Long userId, String avatarUrl);

    // 更新用户信息
    ApiResponse<?> updateProfile(User updatedUser);

    // 修改密码
    ApiResponse<?> changePassword(Long userId, String currentPassword, String newPassword);

    // 新添加的方法
    UserDTO findById(Long id);

    // 更新用户信息
    User updateUser(Long id, UserUpdateRequest request);

    // 获取用户列表
    Page<User> getUserList(String username, String role, Pageable pageable);

    // 添加用户
    User addUser(UserRequest request);

    // 删除用户
    void deleteUser(Long id);

    // 设置用户角色和会员时间
    User setUserRole(Long id, UserRole role, LocalDateTime membershipEndTime);

    // 获取用户总数
    long count();

    // 获取指定时间后创建的用户数
    long countByCreatedAtAfter(LocalDateTime dateTime);

    // 获取指定角色的用户数
    long countByUserRole(UserRole role);

    // 发送邮箱验证码
    ApiResponse<?> sendEmailVerificationCode(String email);

    // 验证邮箱验证码
    ApiResponse<?> verifyEmailCode(String email, String code);
}