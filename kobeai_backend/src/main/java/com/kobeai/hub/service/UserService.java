package com.kobeai.hub.service;

import com.kobeai.hub.dto.UserDTO;
import com.kobeai.hub.dto.request.RegisterRequest;
import com.kobeai.hub.dto.request.UserUpdateRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.User;
import com.kobeai.hub.model.User.UserRole;

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

    void updateUser(Long id, UserUpdateRequest request);
}